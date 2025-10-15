package com.example.demo.benefit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.benefit.application.dto.CashbackBenefitDTO;
import com.example.demo.benefit.application.dto.DiscountBenefitDTO;
import com.example.demo.benefit.application.dto.PointBenefitDTO;
import com.example.demo.benefit.dto.AvailableCardResponse;
import com.example.demo.benefit.dto.BenefitDetailDTO;
import com.example.demo.benefit.dto.CardWithBenefitResponse;
import com.example.demo.benefit.dto.LimitInfo;
import com.example.demo.benefit.dto.MatchingCardsResponse;
import com.example.demo.benefit.dto.PerformanceInfo;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.repository.BenefitRepository;
import com.example.demo.benefit.util.BenefitConverter;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import com.example.demo.user.entity.UserCardPerformance;
import com.example.demo.user.repository.UserBenefitUsageRepository;
import com.example.demo.user.repository.UserCardPerformanceRepository;
import com.example.demo.user.repository.UserCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimalBenefitService {
    
    private final BenefitRepository benefitRepository;
    private final UserCardPerformanceRepository userCardPerformanceRepository;
    private final UserBenefitUsageRepository userBenefitUsageRepository;
    private final UserCardRepository userCardRepository;
    private final CardRepository cardRepository;
    private final BenefitConverter benefitConverter;
    
    /**
     * 매장명으로 매칭되는 카드 조회 
     */
    @Transactional(readOnly = true)
    public List<CardWithBenefitResponse> getMatchingCards(String storeName, Long userId) {
        
        // 1. 사용자 보유 카드 조회
        List<Long> userCardIds = userCardRepository.findCardIdsByUserId(userId);
        
        if (userCardIds.isEmpty()) {
            log.info("사용자 보유 카드가 없습니다. 사용자: {}", userId);
            return List.of();
        }
        
        // 2. 보유 카드 중에서 해당 매장명에 적용되는 혜택들 조회
        List<Benefit> applicableBenefits = benefitRepository.findByApplicableTargetsContaining(storeName);
        
        if (applicableBenefits.isEmpty()) {
            return List.of();
        }
        
        // 3. 보유 카드 중에서만 매칭되는 카드 반환 (카드별로 모든 Benefit 합치기)
        Map<Long, List<Benefit>> benefitsByCard = applicableBenefits.stream()
            .filter(benefit -> userCardIds.contains(benefit.getCard().getId()))
            .collect(Collectors.groupingBy(benefit -> benefit.getCard().getId()));
        
        List<CardWithBenefitResponse> matchingCards = benefitsByCard.entrySet().stream()
            .map(entry -> {
                Long cardId = entry.getKey();
                List<Benefit> cardBenefits = entry.getValue();
                
                //  카드 정보 직접 조회
                Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new RuntimeException("카드를 찾을 수 없습니다: " + cardId));
                
                return new CardWithBenefitResponse(
                    card.getId(),
                    card.getName(),
                    card.getImgUrl(),
                    benefitConverter.convertMultipleBenefitsToDTO(cardBenefits),
                    card.getCardCompany(),
                    card.getCardType()
                );
            })
            .collect(java.util.stream.Collectors.toList());
        
        log.info("매칭된 카드 수: {} 개", matchingCards.size());
        return matchingCards;
    }
    
    /**
     * 실시간 필터링이 적용된 매칭 카드 조회 
     * 1. 실적 검증: 목표 실적을 달성한 카드만
     * 2. 한도 검증: 혜택 한도 내에서 사용 가능한 카드만
     * 3. 실시간 반영: 푸시 알림 기반 자동 혜택 적용 후 즉시 반영
     */
    @Transactional(readOnly = true)
    public MatchingCardsResponse getMatchingCardsWithRealTimeFilter(String storeName, Long userId) {
        
        // 1. 기본 매칭 카드 조회
        List<CardWithBenefitResponse> allMatchingCards = getMatchingCards(storeName, userId);
        
        if (allMatchingCards.isEmpty()) {
            return new MatchingCardsResponse(new ArrayList<>());
        }
        
        // 2. 실시간 필터링 적용 및 분류
        List<AvailableCardResponse> availableCards = new ArrayList<>();
        
        for (CardWithBenefitResponse cardResponse : allMatchingCards) {
            try {
                // 실적 정보 조회
                PerformanceInfo performanceInfo = getPerformanceInfo(userId, cardResponse.cardId());
                
                // 한도 정보 조회
                LimitInfo limitInfo = getLimitInfo(userId, cardResponse.cardId(), cardResponse.benefits());
                
                // 실적 검증
                if (!performanceInfo.isAchieved()) {
                    log.debug("실적 미달성으로 제외: {}", cardResponse.cardName());
                    continue;
                }
                
                // 한도 검증
                if (!hasAvailableBenefitLimit(userId, cardResponse.cardId(), cardResponse.benefits())) {
                    log.debug("한도 부족으로 제외: {}", cardResponse.cardName());
                    continue;
                }
                
                // 사용 가능한 카드
                availableCards.add(createAvailableCardResponse(cardResponse, performanceInfo, limitInfo));
                
            } catch (Exception e) {
                log.error("카드 필터링 중 오류: {}", cardResponse.cardName(), e);
            }
        }
        
        return new MatchingCardsResponse(availableCards);
    }
    
    
    // 혜택 한도 사용 여부 확인
    private boolean hasAvailableBenefitLimit(Long userId, Long cardId, BenefitDetailDTO benefits) {
        // 할인 혜택 한도 확인
        if (benefits.discounts() != null && !benefits.discounts().isEmpty()) {
            Long usedDiscountAmount = userBenefitUsageRepository
                    .findTotalUsedAmountByUserAndCardAndBenefitType(userId, cardId, "DISCOUNT");
            
            for (DiscountBenefitDTO discount : benefits.discounts()) {
                if (usedDiscountAmount + (long) discount.amount() > (long) discount.benefitLimit()) {
                    return false;
                }
            }
        }
        
        // 포인트 혜택 한도 확인
        if (benefits.points() != null && !benefits.points().isEmpty()) {
            Long usedPointAmount = userBenefitUsageRepository
                    .findTotalUsedAmountByUserAndCardAndBenefitType(userId, cardId, "POINT");
            
            for (PointBenefitDTO point : benefits.points()) {
                if (usedPointAmount > point.benefitLimit()) {
                    return false;
                }
            }
        }
        
        // 캐시백 혜택 한도 확인
        if (benefits.cashbacks() != null && !benefits.cashbacks().isEmpty()) {
            Long usedCashbackAmount = userBenefitUsageRepository
                    .findTotalUsedAmountByUserAndCardAndBenefitType(userId, cardId, "CASHBACK");
            
            for (CashbackBenefitDTO cashback : benefits.cashbacks()) {
                if (usedCashbackAmount + (long) cashback.amount() > (long) cashback.benefitLimit()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    
    /**
     * 실적 정보 조회
     */
    private PerformanceInfo getPerformanceInfo(Long userId, Long cardId) {
        Optional<UserCardPerformance> performance = userCardPerformanceRepository
                .findByUserIdAndCardId(userId, cardId);
        
        if (performance.isEmpty()) {
            return new PerformanceInfo(0L, 0L, false);
        }
        
        UserCardPerformance perf = performance.get();
        return new PerformanceInfo(
                perf.getCurrentAmount(),
                perf.getTargetAmount(),
                perf.getIsTargetAchieved()
        );
    }
    
    /**
     * 한도 정보 조회
     */
    private LimitInfo getLimitInfo(Long userId, Long cardId, BenefitDetailDTO benefits) {
        Long usedDiscountAmount = 0L;
        Long usedPointAmount = 0L;
        Long usedCashbackAmount = 0L;
        
        Long totalDiscountLimit = 0L;
        Long totalPointLimit = 0L;
        Long totalCashbackLimit = 0L;
        
        // 할인 혜택 한도 계산
        if (benefits.discounts() != null && !benefits.discounts().isEmpty()) {
            usedDiscountAmount = userBenefitUsageRepository
                    .findTotalUsedAmountByUserAndCardAndBenefitType(userId, cardId, "DISCOUNT");
            totalDiscountLimit = benefits.discounts().stream()
                    .mapToLong(d -> (long) d.benefitLimit())
                    .sum();
        }
        
        // 포인트 혜택 한도 계산
        if (benefits.points() != null && !benefits.points().isEmpty()) {
            usedPointAmount = userBenefitUsageRepository
                    .findTotalUsedAmountByUserAndCardAndBenefitType(userId, cardId, "POINT");
            totalPointLimit = benefits.points().stream()
                    .mapToLong(p -> p.benefitLimit())
                    .sum();
        }
        
        // 캐시백 혜택 한도 계산
        if (benefits.cashbacks() != null && !benefits.cashbacks().isEmpty()) {
            usedCashbackAmount = userBenefitUsageRepository
                    .findTotalUsedAmountByUserAndCardAndBenefitType(userId, cardId, "CASHBACK");
            totalCashbackLimit = benefits.cashbacks().stream()
                    .mapToLong(c -> c.benefitLimit())
                    .sum();
        }
        
        return new LimitInfo(
                usedDiscountAmount,
                totalDiscountLimit,
                usedPointAmount,
                totalPointLimit,
                usedCashbackAmount,
                totalCashbackLimit
        );
    }
    
    /**
     * 사용 가능한 카드 응답 생성
     */
    private AvailableCardResponse createAvailableCardResponse(
            CardWithBenefitResponse cardResponse, PerformanceInfo performanceInfo, LimitInfo limitInfo) {
        return AvailableCardResponse.builder()
               .cardId(cardResponse.cardId())
                .cardName(cardResponse.cardName())
                .imgUrl(cardResponse.imgUrl())
                .benefits(cardResponse.benefits())
                .performance(performanceInfo)
                .limits(limitInfo)
                .build();
    }

    
}
