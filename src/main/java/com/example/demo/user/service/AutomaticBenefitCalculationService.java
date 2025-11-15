package com.example.demo.user.service;

import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.expense.entity.Expense;
import com.example.demo.user.entity.UserCardPerformance;
import com.example.demo.user.entity.UserBenefitUsage;
import com.example.demo.user.repository.UserCardPerformanceRepository;
import com.example.demo.user.repository.UserBenefitUsageRepository;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.repository.BenefitRepository;
import com.example.demo.card.entity.Card;
import com.example.demo.user.entity.UserCard;
import com.example.demo.user.repository.UserCardRepository;
import com.example.demo.lock.RedisLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomaticBenefitCalculationService {
    
    private final UserCardPerformanceRepository userCardPerformanceRepository;
    private final UserBenefitUsageRepository userBenefitUsageRepository;
    private final BenefitRepository benefitRepository;
    private final UserCardRepository userCardRepository;
    private final RedisLockUtil redisLockUtil;
    
    /**
     * 소비내역 저장 후 자동으로 실적과 혜택 계산
     * 푸시 알림으로 받은 소비내역을 기반으로 자동 처리
     */
    @Transactional
    public void processExpenseAndCalculateBenefits(Expense expense) {
        log.info("소비내역 기반 자동 혜택 계산 시작 - 장소: {}, 금액: {}", expense.getPlace(), expense.getAmount());
        
        // 1. 사용자 보유 카드 조회
        List<Card> userCards =getUserCards(expense.getUserId());
        
        for (Card card : userCards) {
            try {
                // 2. 실적 업데이트
                updateCardPerformance(expense.getUserId(), card.getId(), expense.getAmount());
                
                // 3. 해당 카드의 혜택이 적용되는지 확인 및 적용
                if (isBenefitApplicable(expense, card)) {
                    applyBestBenefitForExpense(expense, card);
                }

                
            } catch (Exception e) {
                log.error("카드 {} 혜택 계산 실패", card.getName(), e);
            }
        }
        
        log.info("소비내역 기반 자동 혜택 계산 완료");
    }

    // 가장 유리한 혜택 하나 찾아서 적용
    private void applyBestBenefitForExpense(Expense expense, Card card){
        // 실적 달성 여부 확인
        Optional<UserCardPerformance> performanceOpt = userCardPerformanceRepository.findByUserIdAndCardId(expense.getUserId(), card.getId());

        if (performanceOpt.isEmpty() || !performanceOpt.get().getIsTargetAchieved()) {
            log.info("카드 {} 실적 미달성으로 혜택 적용 스킵", card.getName());
            return;
        }

        // 해당 장소에 적용 가능한 모든 상위 혜택 조회
        List<Benefit> applicableBenefits = benefitRepository.findByCardIdAndPlace(card.getId(), expense.getPlace());

        if(applicableBenefits.isEmpty()){
            return;
        }

        // 적용 가능한 하위 혜택 중 각각 혜택이 가장 큰 할인, 적립, 캐시백 정보
        findAndApplyBestDiscount(expense, card, applicableBenefits);
        findAndApplyBestPoint(expense, card, applicableBenefits);
        findAndApplyBestCashback(expense, card, applicableBenefits);

    }

    // 할인에서 가장 큰 혜택 하나 찾기
    private void findAndApplyBestDiscount(Expense expense, Card card, List<Benefit> benefits) {
        benefits.stream()
                .flatMap(b -> b.getDiscountBenefits().stream())
                .max(Comparator.comparingDouble(db -> calculateDiscount(expense.getAmount(), db)))
                .ifPresent(bestDiscount -> applyDiscountBenefit(expense, card, bestDiscount));
    }

    private void findAndApplyBestPoint(Expense expense, Card card, List<Benefit> benefits) {
        benefits.stream()
                .flatMap(b -> b.getPointBenefits().stream())
                .max(Comparator.comparingDouble(pb -> calculatePoint(expense.getAmount(), pb)))
                .ifPresent(bestPoint -> applyPointBenefit(expense, card, bestPoint));
    }

    private void findAndApplyBestCashback(Expense expense, Card card, List<Benefit> benefits) {
        benefits.stream()
                .flatMap(b -> b.getCashbackBenefits().stream())
                .max(Comparator.comparingDouble(cb -> calculateCashback(expense.getAmount(), cb)))
                .ifPresent(bestCashback -> applyCashbackBenefit(expense, card, bestCashback));
    }

    
    /**
     * 사용자 보유 카드 조회
     */
    private List<Card> getUserCards(Long userId) {
        // UserCard를 통해 사용자 보유 카드 조회
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
        return userCards.stream()
                .map(UserCard::getCard)
                .toList();
    }
    
        /**
         * 카드 실적 업데이트
         * 
         * 분산락 적용 이유:
         * 1. 동일한 카드에 대한 동시 실적 업데이트 방지
         * 2. 실적 계산 오류 방지 (Race Condition)
         * 3. 목표 달성 여부 잘못 판단 방지
         * 4. 여러 소비내역이 동시에 처리될 때 일관성 보장
         */
        private void updateCardPerformance(Long userId, Long cardId, Long amount) {
            String lockKey = String.format("performance:%d:%d", userId, cardId);
            
            redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processCardPerformanceUpdate(userId, cardId, amount);
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                2, // 최대 재시도 2회
                50, // 재시도 간격 50ms
                10 // TTL 10초
            );
        }
    
    /**
     * 실제 실적 업데이트 로직 (분산락 내부에서 실행)
     */
    private void processCardPerformanceUpdate(Long userId, Long cardId, Long amount) {
        Optional<UserCardPerformance> performanceOpt = userCardPerformanceRepository
                .findByUserIdAndCardId(userId, cardId);
        
        if (performanceOpt.isPresent()) {
            UserCardPerformance performance = performanceOpt.get();
            Long newAmount = performance.getCurrentAmount() + amount;
            boolean isTargetAchieved = newAmount >= performance.getTargetAmount();
            
            UserCardPerformance updatedPerformance = performance.builder()
                    .currentAmount(newAmount)
                    .isTargetAchieved(isTargetAchieved)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();
            
            userCardPerformanceRepository.save(updatedPerformance);
            
            log.info("카드 실적 업데이트 - 카드: {}, 현재: {}, 목표: {}, 달성: {}", 
                    cardId, newAmount, performance.getTargetAmount(), isTargetAchieved);
        }
    }
    
    /**
     * 혜택 적용 가능 여부 확인
     */
    private boolean isBenefitApplicable(Expense expense, Card card) {
        // 1. 실적 달성 확인
        Optional<UserCardPerformance> performance = userCardPerformanceRepository
                .findByUserIdAndCardId(expense.getUserId(), card.getId());
        
        if (performance.isEmpty() || !performance.get().getIsTargetAchieved()) {
            return false;
        }
        
        // 2. 해당 장소에 적용되는 혜택이 있는지 확인
        List<Benefit> applicableBenefits = benefitRepository.findByCardIdAndPlace(card.getId(), expense.getPlace());
        
        return !applicableBenefits.isEmpty();
    }

    
        /**
         * 할인 혜택 자동 적용
         * 
         * 분산락 적용 이유:
         * 1. 동일한 혜택에 대한 중복 적용 방지
         * 2. 한도 초과 혜택 적용 방지
         * 3. 여러 소비내역이 동시에 처리될 때 한도 계산 오류 방지
         * 4. 사용자-카드-혜택타입별 동시성 제어
         */
        private void applyDiscountBenefit(Expense expense, Card card, DiscountBenefit benefit) {
            String lockKey = String.format("benefit:%d:%d:DISCOUNT", expense.getUserId(), card.getId());
            
            redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processBenefitApplication(expense, card, benefit, "DISCOUNT");
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                1, // 최대 재시도 1회
                50, // 재시도 간격 50ms
                5 // TTL 5초
            );
        }


    /**
     * 포인트 혜택 자동 적용
     *
     * 분산락 적용 이유:
     * 1. 동일한 혜택에 대한 중복 적용 방지
     * 2. 한도 초과 혜택 적용 방지
     * 3. 여러 소비내역이 동시에 처리될 때 한도 계산 오류 방지
     * 4. 사용자-카드-혜택타입별 동시성 제어
     */
    private void applyPointBenefit(Expense expense, Card card, PointBenefit benefit) {
        String lockKey = String.format("benefit:%d:%d:POINT", expense.getUserId(), card.getId());

        redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processBenefitApplication(expense, card, benefit, "POINT");
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                1, // 최대 재시도 1회
                50, // 재시도 간격 50ms
                5 // TTL 5초
        );
    }


    /**
     * 캐시백 혜택 자동 적용
     *
     * 분산락 적용 이유:
     * 1. 동일한 혜택에 대한 중복 적용 방지
     * 2. 한도 초과 혜택 적용 방지
     * 3. 여러 소비내역이 동시에 처리될 때 한도 계산 오류 방지
     * 4. 사용자-카드-혜택타입별 동시성 제어
     */
    private void applyCashbackBenefit(Expense expense, Card card, CashbackBenefit benefit) {
        String lockKey = String.format("benefit:%d:%d:CASHBACK", expense.getUserId(), card.getId());

        redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processBenefitApplication(expense, card, benefit, "CASHBACK");
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                1, // 최대 재시도 1회
                50, // 재시도 간격 50ms
                5 // TTL 5초
        );
    }


    
    /**
     * 실제 혜택 적용 공통 로직(분산락 내부에서 실행)
     */
    private void processBenefitApplication(Expense expense, Card card, Object benefitDetail, String benefitType) {

        long benefitLimit = 0;
        long calculatedAmount = 0;
        long benefitDetailId = 0;

        if (benefitDetail instanceof DiscountBenefit db) {
            benefitLimit = (long) db.getBenefitLimit();
            calculatedAmount = calculateDiscount(expense.getAmount(), db);
            benefitDetailId = db.getId();
        } else if (benefitDetail instanceof PointBenefit pb) {
            benefitLimit = pb.getBenefitLimit();
            calculatedAmount = calculatePoint(expense.getAmount(), pb);
            benefitDetailId = pb.getId();
        } else if (benefitDetail instanceof CashbackBenefit cb) {
            benefitLimit = (long) cb.getBenefitLimit();
            calculatedAmount = calculateCashback(expense.getAmount(), cb);
            benefitDetailId = cb.getId();
        }

        if (calculatedAmount <= 0) return;

        // 한도 검사
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Long currentUsage = userBenefitUsageRepository.getUsageAmountInPeriod(
                expense.getUserId(), card.getId(), benefitDetailId, benefitType, startDate, endDate);

        if (currentUsage + calculatedAmount > benefitLimit) {
            log.info("혜택 한도 초과 - 카드: {}, 혜택ID: {}, 타입: {}", card.getName(), benefitDetailId, benefitType);
            return;
        }
        UserBenefitUsage usage = UserBenefitUsage.builder()
                .member(expense.getMember())
                .card(card)
                .benefitDetailId(benefitDetailId) // 실제 하위 혜택 ID 저장
                .benefitType(benefitType)
                .usedAmount(calculatedAmount)
                .remainingLimit(benefitLimit - (currentUsage + calculatedAmount)) // 남은 한도 계산
                .place(expense.getPlace())
                .usedAt(expense.getPostedAt())
                .build();

        userBenefitUsageRepository.save(usage);
        log.info("{} 혜택 자동 적용 - 카드: {}, 적용금액: {}", benefitType, card.getName(), calculatedAmount);




    }

    private long calculateDiscount(Long expenseAmount, DiscountBenefit db) {
        return (long) (expenseAmount * db.getRate());
    }
    private long calculatePoint(Long expenseAmount, PointBenefit pb) {
        return (long) (expenseAmount * pb.getRate());
    }
    private long calculateCashback(Long expenseAmount, CashbackBenefit cb) {
        return (long) (expenseAmount * cb.getRate());
    }



}
