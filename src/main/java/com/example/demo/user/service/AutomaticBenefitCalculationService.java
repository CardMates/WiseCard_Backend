package com.example.demo.user.service;

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
        List<Card> userCards = getUserCards(expense.getUserId());
        
        for (Card card : userCards) {
            try {
                // 2. 실적 업데이트
                updateCardPerformance(expense.getUserId(), card.getId(), expense.getAmount());
                
                // 3. 해당 카드의 혜택이 적용되는지 확인
                if (isBenefitApplicable(expense, card)) {
                    // 4. 혜택 자동 적용
                    applyAutomaticBenefit(expense, card);
                }
                
            } catch (Exception e) {
                log.error("카드 {} 혜택 계산 실패", card.getName(), e);
            }
        }
        
        log.info("소비내역 기반 자동 혜택 계산 완료");
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
     * 자동 혜택 적용
     */
    private void applyAutomaticBenefit(Expense expense, Card card) {
        List<Benefit> applicableBenefits = benefitRepository.findByCardIdAndPlace(card.getId(), expense.getPlace());
        
        for (Benefit benefit : applicableBenefits) {
            // 혜택 타입별 자동 적용
            if (benefit.getDiscountBenefits() != null && !benefit.getDiscountBenefits().isEmpty()) {
                applyDiscountBenefit(expense, card, benefit);
            }
            
            if (benefit.getPointBenefits() != null && !benefit.getPointBenefits().isEmpty()) {
                applyPointBenefit(expense, card, benefit);
            }
            
            if (benefit.getCashbackBenefits() != null && !benefit.getCashbackBenefits().isEmpty()) {
                applyCashbackBenefit(expense, card, benefit);
            }
        }
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
        private void applyDiscountBenefit(Expense expense, Card card, Benefit benefit) {
            String lockKey = String.format("benefit:%d:%d:DISCOUNT", expense.getUserId(), card.getId());
            
            redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processDiscountBenefit(expense, card, benefit);
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                1, // 최대 재시도 1회
                50, // 재시도 간격 50ms
                5 // TTL 5초
            );
        }
    
    /**
     * 실제 할인 혜택 적용 로직 (분산락 내부에서 실행)
     */
    private void processDiscountBenefit(Expense expense, Card card, Benefit benefit) {
        // 할인 혜택 계산 및 적용
        Long discountAmount = calculateDiscountAmount(expense.getAmount(), benefit);
        
        if (discountAmount > 0) {
            UserBenefitUsage usage = UserBenefitUsage.builder()
                    .userId(expense.getUserId())
                    .card(card)
                    .benefit(benefit)
                    .benefitType("DISCOUNT")
                    .usedAmount(discountAmount)
                    .remainingLimit(0L) // 계산 필요
                    .place(expense.getPlace())
                    .usedAt(expense.getPostedAt())
                    .build();
            
            userBenefitUsageRepository.save(usage);
            
            log.info("할인 혜택 자동 적용 - 카드: {}, 할인금액: {}", card.getName(), discountAmount);
        }
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
        private void applyPointBenefit(Expense expense, Card card, Benefit benefit) {
            String lockKey = String.format("benefit:%d:%d:POINT", expense.getUserId(), card.getId());
            
            redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processPointBenefit(expense, card, benefit);
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                1, // 최대 재시도 1회
                50, // 재시도 간격 50ms
                5 // TTL 5초
            );
        }
    
    /**
     * 실제 포인트 혜택 적용 로직 (분산락 내부에서 실행)
     */
    private void processPointBenefit(Expense expense, Card card, Benefit benefit) {
        // 포인트 혜택 계산 및 적용
        Long pointAmount = calculatePointAmount(expense.getAmount(), benefit);
        
        if (pointAmount > 0) {
            UserBenefitUsage usage = UserBenefitUsage.builder()
                    .userId(expense.getUserId())
                    .card(card)
                    .benefit(benefit)
                    .benefitType("POINT")
                    .usedAmount(pointAmount)
                    .remainingLimit(0L) // 계산 필요
                    .place(expense.getPlace())
                    .usedAt(expense.getPostedAt())
                    .build();
            
            userBenefitUsageRepository.save(usage);
            
            log.info("포인트 혜택 자동 적용 - 카드: {}, 포인트: {}", card.getName(), pointAmount);
        }
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
        private void applyCashbackBenefit(Expense expense, Card card, Benefit benefit) {
            String lockKey = String.format("benefit:%d:%d:CASHBACK", expense.getUserId(), card.getId());
            
            redisLockUtil.acquireAndRunLock(
                lockKey,
                () -> {
                    processCashbackBenefit(expense, card, benefit);
                    return null; // void 메서드를 Supplier로 사용하기 위해 null 반환
                },
                1, // 최대 재시도 1회
                50, // 재시도 간격 50ms
                5 // TTL 5초
            );
        }
    
    /**
     * 실제 캐시백 혜택 적용 로직 (분산락 내부에서 실행)
     */
    private void processCashbackBenefit(Expense expense, Card card, Benefit benefit) {
        // 캐시백 혜택 계산 및 적용
        Long cashbackAmount = calculateCashbackAmount(expense.getAmount(), benefit);
        
        if (cashbackAmount > 0) {
            UserBenefitUsage usage = UserBenefitUsage.builder()
                    .userId(expense.getUserId())
                    .card(card)
                    .benefit(benefit)
                    .benefitType("CASHBACK")
                    .usedAmount(cashbackAmount)
                    .remainingLimit(0L) // 계산 필요
                    .place(expense.getPlace())
                    .usedAt(expense.getPostedAt())
                    .build();
            
            userBenefitUsageRepository.save(usage);
            
            log.info("캐시백 혜택 자동 적용 - 카드: {}, 캐시백: {}", card.getName(), cashbackAmount);
        }
    }
    
    /**
     * 할인 금액 계산
     */
    private Long calculateDiscountAmount(Long expenseAmount, Benefit benefit) {
        // 할인 혜택 계산 로직
        return benefit.getDiscountBenefits().stream()
                .mapToLong(db -> (long) (expenseAmount * db.getRate()))
                .sum();
    }
    
    /**
     * 포인트 금액 계산
     */
    private Long calculatePointAmount(Long expenseAmount, Benefit benefit) {
        // 포인트 혜택 계산 로직
        return benefit.getPointBenefits().stream()
                .mapToLong(pb -> (long) (expenseAmount * pb.getRate()))
                .sum();
    }
    
    /**
     * 캐시백 금액 계산
     */
    private Long calculateCashbackAmount(Long expenseAmount, Benefit benefit) {
        // 캐시백 혜택 계산 로직
        return benefit.getCashbackBenefits().stream()
                .mapToLong(cb -> (long) (expenseAmount * cb.getRate()))
                .sum();
    }
}
