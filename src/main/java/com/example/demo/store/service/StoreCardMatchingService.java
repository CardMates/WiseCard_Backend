package com.example.demo.store.service;

import com.example.demo.benefit.dto.BenefitResponse;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.application.dto.ChannelType;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.card.entity.Card;
import com.example.demo.store.dto.BenefitInfoDTO;
import com.example.demo.store.dto.CardBenefitDTO;
import com.example.demo.store.dto.StoreInfoDTO;
import com.example.demo.user.entity.UserCardPerformance;
import com.example.demo.user.repository.UserBenefitUsageRepository;
import com.example.demo.user.repository.UserCardPerformanceRepository;
import com.example.demo.user.repository.UserCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreCardMatchingService {

    private final UserCardPerformanceRepository userCardPerformanceRepository;
    private final UserBenefitUsageRepository userBenefitUsageRepository;

    public List<StoreInfoDTO> matchStoresWithCards(List<Map<String, Object>> kakaoPlaces, List<Card> userCards, Long userId) {
        return matchStoresWithCards(kakaoPlaces, userCards, userId, null);
    }

    public List<StoreInfoDTO> matchStoresWithCards(List<Map<String, Object>> kakaoPlaces, List<Card> userCards, Long userId, ChannelType channelType) {
        List<StoreInfoDTO> storesWithCards = new ArrayList<>();

        for (Map<String, Object> store : kakaoPlaces) {
            List<CardBenefitDTO> availableCards = findMatchingCards(userCards, store, userId,channelType);

            if (!availableCards.isEmpty()) {
                StoreInfoDTO storeInfo = StoreInfoDTO.builder()
                    .id((String) store.get("id"))
                    .placeName((String) store.get("place_name"))
                    .availableCards(availableCards)
                        .lat((Double) store.get("y"))
                        .lng((Double) store.get("x"))
                    .build();
                storesWithCards.add(storeInfo);
            }
        }

        return storesWithCards;
    }


    private List<CardBenefitDTO> findMatchingCards(List<Card> userCards, Map<String, Object> store, Long userId, ChannelType channelType) {

        // 최종 반환될, 사용 가능한 카드 목록
        List<CardBenefitDTO> availableCards = new ArrayList<>();
        String storeName = (String) store.get("place_name");
        String categoryCode = (String) store.get("category_group_code");

        log.info("🔍 매장 매칭 시작 - 매장명: {}, 카테고리: {}", storeName, categoryCode);

        // 사용자의 모든 카드에 대해 반복
        for (Card card : userCards) {

            // '이번 카드'에 대한 '혜택 상품'들만 담을 '임시' 리스트를 만듭니다.
            List<BenefitInfoDTO> benefitsForThisCard = new ArrayList<>();

            // 해당 카드의 전월 실적 DB에서 조회
            Optional<UserCardPerformance> performance = userCardPerformanceRepository.findByUserIdAndCardId(userId, card.getId());
            Long userCurrentSpending = performance.map(UserCardPerformance::getCurrentAmount).orElse(0L);


            // 카드가 가진 모든 상위 혜택에 대해 반복
            for (Benefit benefit : card.getBenefits()) {
                log.info("🎁 혜택 검사: {} (카테고리: {})", benefit.getSummary(), benefit.getApplicableCategory());

                // 필터 1: 매장 적용 가능 여부 검사
                if(!isBenefitApplicable(benefit, storeName, categoryCode, channelType)) {
                    continue; // 이 혜택은 이 매장에서 사용할 수 없으므로 다음 혜택으로 넘어감
                }

                // 이 상위 혜택에 속한 개별 하위 혜택을 각각 검사
                // 할인
                for (DiscountBenefit discount : benefit.getDiscountBenefits()){
                    // 필터 2: 사용자의 실적 조건 만족 검사
                    boolean isPerformanceMet = discount.getMinimumSpending() <= userCurrentSpending;
                    if (!isPerformanceMet) continue;

                    // 필터 3: 할인 혜택의 월 한도가 남아있는지 검사
                    Long usage = getUsageForCurrentMonth(userId, card.getId(), discount.getId(), "DISCOUNT");
                    if (usage < discount.getBenefitLimit()){
                        // 모든 필터 통과, 이 혜택은 사용 가능
                        benefitsForThisCard.add(
                                createBenefitInfo(benefit, discount, "DISCOUNT", discount.getBenefitLimit()-usage

                        ));
                    }
                }
                // 포인트
                for (PointBenefit point : benefit.getPointBenefits()){
                    // 필터 2: 사용자의 실적 조건 만족 검사
                    boolean isPerformanceMet = point.getMinimumSpending() <= userCurrentSpending;
                    if (!isPerformanceMet) continue;
                    // 필터 3: 포인트
                    Long usage = getUsageForCurrentMonth(userId, card.getId(), point.getId(), "POINT");
                    if (usage < point.getBenefitLimit()){
                        // 모든 필터 통과, 이 혜택은 사용 가능
                        benefitsForThisCard.add(
                                createBenefitInfo(benefit, point, "POINT", point.getBenefitLimit() -usage
                                ));
                    }
                }
                // 캐시백
                for (CashbackBenefit cashback : benefit.getCashbackBenefits()){
                    // 필터 2: 사용자의 실적 조건 만
                    boolean isPerformanceMet = cashback.getMinimumSpending() <= userCurrentSpending;
                    if (!isPerformanceMet) continue;
                    // 필터 3: 캐시백
                    Long usage = getUsageForCurrentMonth(userId, card.getId(), cashback.getId(), "CASHBACK");
                    if (usage < cashback.getBenefitLimit()){
                        // 모든 필터 통과, 이 혜택은 사용 가능
                        benefitsForThisCard.add(
                                createBenefitInfo(benefit, cashback, "CASHBACK", cashback.getBenefitLimit() -usage
                                        ));
                    }
                }


            }
            if(!benefitsForThisCard.isEmpty()){
                CardBenefitDTO cardBenefitDTO = CardBenefitDTO.builder()
                        .cardId(card.getId())
                        .cardName(card.getName())
                        .benefits(benefitsForThisCard) // 리스트
                        .build();
                availableCards.add(cardBenefitDTO);
            }

        }

        return availableCards;
    }

    // 이번 달 누적 사용량을 조회하는 헬퍼 메서드
    private Long getUsageForCurrentMonth(Long userId, Long cardId, Long benefitDetailId, String benefitType) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // UserBenefitUsage 엔티티 사용
        return userBenefitUsageRepository.getUsageAmountInPeriod(
                userId, cardId, benefitDetailId, benefitType, startDate, endDate
        );
    }

    /**
     * 상위 Benefit과 하위 혜택(Discount, Point 등) 객체를 조합하여
     * 최종 응답에 사용될 BenefitInfoDTO를 생성하는 헬퍼 메서드입니다.
     *
     * @param parent 혜택의 요약(summary) 등 공통 정보를 가진 상위 Benefit 객체
     * @param child 할인율, 한도 등 구체적인 숫자 정보를 가진 하위 혜택 객체 (Object 타입)
     * @param type "DISCOUNT", "POINT", "CASHBACK" 등 혜택의 종류
     * @param remainingLimit 서비스 로직에서 미리 계산된, 이 혜택의 남은 월간 한도
     * @return 모든 정보가 조립된 BenefitInfoDTO 객체
     */
    private BenefitInfoDTO createBenefitInfo(Benefit parent, Object child, String type, Long remainingLimit) {

        // DTO 조립
        BenefitInfoDTO.BenefitInfoDTOBuilder benefitInfoDTOBuilder = BenefitInfoDTO.builder()
                .benefitId(parent.getId())
                .benefitType(type)
                .summary(parent.getSummary())
                .remainingLimit(remainingLimit);

        if (child instanceof DiscountBenefit discount) {
            // 할인 혜택이라면 할인율(rate)과 고정 할인액(amount) 정보를 추가
            benefitInfoDTOBuilder.rate(discount.getRate());
            benefitInfoDTOBuilder.amount(discount.getAmount());
        } else if (child instanceof PointBenefit point) {
            benefitInfoDTOBuilder.rate(point.getRate());
        } else if (child instanceof CashbackBenefit cashback) {
            benefitInfoDTOBuilder.rate(cashback.getRate());
            benefitInfoDTOBuilder.amount(cashback.getAmount());

        }
        return benefitInfoDTOBuilder.build();

    }


    private boolean isBenefitApplicable(Benefit benefit, String storeName, String categoryCode, ChannelType channelType) {
        // 카테고리 매칭 확인
        if (benefit.getApplicableCategory() != null && !benefit.getApplicableCategory().isEmpty()) {
            if (!benefit.getApplicableCategory().contains(categoryCode)) {
                return false;
            }
        }

        // ChannelType 검증
        if (channelType != null) {
            return hasChannelType(benefit, channelType);
        }

        return true;
    }

    private boolean hasChannelType(Benefit benefit, ChannelType channelType) {
        return benefit.getDiscountBenefits().stream().anyMatch(db -> 
                db.getChannel() == channelType || db.getChannel() == ChannelType.BOTH) ||
               benefit.getPointBenefits().stream().anyMatch(pb -> 
                pb.getChannel() == channelType || pb.getChannel() == ChannelType.BOTH) ||
               benefit.getCashbackBenefits().stream().anyMatch(cb -> 
                cb.getChannel() == channelType || cb.getChannel() == ChannelType.BOTH);
    }
}