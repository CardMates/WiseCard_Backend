package com.example.demo.benefit.service;

import com.example.demo.benefit.dto.AvailableCardResponse;
import com.example.demo.benefit.dto.BenefitResponse;
import com.example.demo.benefit.dto.MatchingCardsResponse;
import com.example.demo.benefit.dto.PerformanceInfo;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.card.entity.Card;
import com.example.demo.store.service.KakaoMapService;
import com.example.demo.user.entity.UserCardPerformance;
import com.example.demo.user.repository.UserBenefitUsageRepository;
import com.example.demo.user.repository.UserCardPerformanceRepository;
import com.example.demo.user.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimalBenefitService {

    private final UserCardRepository userCardRepository;
    private final UserCardPerformanceRepository userCardPerformanceRepository;
    private final UserBenefitUsageRepository userBenefitUsageRepository;
    private final KakaoMapService kakaoMapService; // 카테고리 조회를 위해 추가

    @Transactional(readOnly = true)
    public MatchingCardsResponse getMatchingCardsWithRealTimeFilter(String storeName, Long userId) {

        // 1. [개선된 검색] KakaoMapService를 통해 가게의 정확한 정보(특히 카테고리)를 조회
        List<Map<String, Object>> places = kakaoMapService.searchPlacesByCategory(storeName);
        if (places.isEmpty()) {
            log.warn("가게 정보를 찾을 수 없습니다: {}", storeName);
            return new MatchingCardsResponse(new ArrayList<>());
        }
        // 첫 번째 검색 결과를 기준으로 삼음
        Map<String, Object> storeInfo = places.get(0);
        String storeCategory = (String) storeInfo.get("category_group_code");

        // 2. 사용자 보유 카드 조회
        List<Card> userCards = userCardRepository.findByUserId(userId);
        if (userCards.isEmpty()) {
            return new MatchingCardsResponse(new ArrayList<>());
        }

        List<AvailableCardResponse> availableCardsResult = new ArrayList<>();

        // 3. 카드별 루프
        for (Card card : userCards) {
            Optional<UserCardPerformance> performanceOpt = userCardPerformanceRepository.findByUserIdAndCardId(userId, card.getId());
            long userCurrentSpending = performanceOpt.map(UserCardPerformance::getCurrentAmount).orElse(0L);

            List<BenefitResponse> usableBenefitsForThisCard = new ArrayList<>();

            // 4. 혜택별 루프
            for (Benefit benefit : card.getBenefits()) {

                // 4-1. [필터 1: 업종] 혜택의 적용 카테고리와 가게의 카테고리가 일치하는지 확인
                if (benefit.getApplicableCategory() == null || !benefit.getApplicableCategory().contains(storeCategory)) {
                    continue;
                }

                // 4-2. 하위 혜택별로 실적 및 한도 검사 (3단계 필터링)
                addApplicableSubBenefits(usableBenefitsForThisCard, benefit, userCurrentSpending, userId, card.getId());
            }

            // 5. 카드별 결과 취합
            if (!usableBenefitsForThisCard.isEmpty()) {
                PerformanceInfo performanceInfo = new PerformanceInfo(
                        userCurrentSpending,
                        performanceOpt.map(UserCardPerformance::getTargetAmount).orElse(0L),
                        performanceOpt.map(UserCardPerformance::getIsTargetAchieved).orElse(false)
                );

                AvailableCardResponse availableCard = AvailableCardResponse.builder()
                        .cardId(card.getId())
                        .cardName(card.getName())
                        .imgUrl(card.getImgUrl())
                        .cardCompany(card.getCardCompany())
                        .benefits(usableBenefitsForThisCard)
                        .performance(performanceInfo)
                        .build();

                availableCardsResult.add(availableCard);
            }
        }

        // 6. 최종 결과 반환
        return new MatchingCardsResponse(availableCardsResult);
    }

    /**
     * 하위 혜택들을 검사하여, 사용 가능한 혜택만 리스트에 추가하는 메서드
     */
    private void addApplicableSubBenefits(List<BenefitResponse> usableBenefits, Benefit benefit, long userSpending, Long userId, Long cardId) {
        // 할인 혜택 검사
        for (DiscountBenefit discount : benefit.getDiscountBenefits()) {
            if (userSpending >= discount.getMinimumSpending()) { // 필터 2: 실적
                Long usage = getUsageForCurrentMonth(userId, cardId, discount.getId(), "DISCOUNT");
                if (usage < discount.getBenefitLimit()) { // 필터 3: 한도
                    usableBenefits.add(createBenefitResponseDTO(benefit, discount, "DISCOUNT", discount.getBenefitLimit() - usage));
                }
            }
        }
        // 포인트 혜택 검사
        for (PointBenefit point : benefit.getPointBenefits()) {
            if (userSpending >= point.getMinimumSpending()) { // 필터 2: 실적
                Long usage = getUsageForCurrentMonth(userId, cardId, point.getId(), "POINT");
                if (usage < point.getBenefitLimit()) { // 필터 3: 한도
                    usableBenefits.add(createBenefitResponseDTO(benefit, point, "POINT", point.getBenefitLimit() - usage));
                }
            }
        }
        // 캐시백 혜택 검사
        for (CashbackBenefit cashback : benefit.getCashbackBenefits()) {
            if (userSpending >= cashback.getMinimumSpending()) { // 필터 2: 실적
                Long usage = getUsageForCurrentMonth(userId, cardId, cashback.getId(), "CASHBACK");
                if (usage < cashback.getBenefitLimit()) { // 필터 3: 한도
                    usableBenefits.add(createBenefitResponseDTO(benefit, cashback, "CASHBACK", cashback.getBenefitLimit() - usage));
                }
            }
        }
    }

    /**
     * 이번 달 누적 사용량을 조회하는 헬퍼 메서드
     */
    private Long getUsageForCurrentMonth(Long userId, Long cardId, Long benefitDetailId, String benefitType) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        return userBenefitUsageRepository.getUsageAmountInPeriod(userId, cardId, benefitDetailId, benefitType, startDate, endDate);
    }

    /**
     * 최종 응답에 사용될 BenefitResponse DTO를 생성하는 헬퍼 메서드
     */
    private BenefitResponse createBenefitResponseDTO(Benefit parent, Object child, String type, Long remainingLimit) {
        BenefitResponse.BenefitResponseBuilder builder = BenefitResponse.builder()
                .benefitId(parent.getId()) // 또는 하위 혜택 ID를 사용해도 됨 (정책에 따라)
                .benefitType(type)
                .summary(parent.getSummary())
                .remainingLimit(remainingLimit);

        if (child instanceof DiscountBenefit db) {
            builder.minimumSpending(db.getMinimumSpending())
                    .benefitLimit(db.getBenefitLimit())
                    .rate(db.getRate())
                    .amount(db.getAmount());
        } else if (child instanceof PointBenefit pb) {
            builder.minimumSpending(pb.getMinimumSpending())
                    .benefitLimit(pb.getBenefitLimit())
                    .rate(pb.getRate());
        } else if (child instanceof CashbackBenefit cb) {
            builder.minimumSpending(cb.getMinimumSpending())
                    .benefitLimit( cb.getBenefitLimit())
                    .rate(cb.getRate())
                    .amount(cb.getAmount());
        }
        return builder.build();
    }
}