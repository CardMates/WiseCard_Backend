package com.example.demo.store.service;

import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.application.dto.ChannelType;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.card.entity.Card;
import com.example.demo.store.dto.BenefitInfoDTO;
import com.example.demo.store.dto.CardBenefitDTO;
import com.example.demo.store.dto.StoreInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreCardMatchingService {

    public List<StoreInfoDTO> matchStoresWithCards(List<Map<String, Object>> kakaoPlaces, List<Card> userCards) {
        return matchStoresWithCards(kakaoPlaces, userCards, null);
    }

    public List<StoreInfoDTO> matchStoresWithCards(List<Map<String, Object>> kakaoPlaces, List<Card> userCards, ChannelType channelType) {
        List<StoreInfoDTO> storesWithCards = new ArrayList<>();

        for (Map<String, Object> store : kakaoPlaces) {
            List<CardBenefitDTO> availableCards = findMatchingCards(userCards, store, channelType);

            if (!availableCards.isEmpty()) {
                StoreInfoDTO storeInfo = StoreInfoDTO.builder()
                    .id((String) store.get("id"))
                    .placeName((String) store.get("place_name"))
                    .availableCards(availableCards)
                    .build();
                storesWithCards.add(storeInfo);
            }
        }

        return storesWithCards;
    }


    private List<CardBenefitDTO> findMatchingCards(List<Card> userCards, Map<String, Object> store, ChannelType channelType) {
        List<CardBenefitDTO> availableCards = new ArrayList<>();
        String storeName = (String) store.get("place_name");
        String categoryCode = (String) store.get("category_group_code");

        log.info("🔍 매장 매칭 시작 - 매장명: {}, 카테고리: {}", storeName, categoryCode);

        for (Card card : userCards) {
            List<BenefitInfoDTO> matchingBenefits = new ArrayList<>();

            for (Benefit benefit : card.getBenefits()) {
                log.info("🎁 혜택 검사: {} (카테고리: {})", benefit.getSummary(), benefit.getApplicableCategory());
                if (isBenefitApplicable(benefit, storeName, categoryCode, channelType)) {
                    log.info("✅ 혜택 매칭됨: {}", benefit.getSummary());
                    List<BenefitInfoDTO> benefitInfos = createBenefitInfoList(benefit, channelType);
                    matchingBenefits.addAll(benefitInfos);
                } else {
                    log.info("❌ 혜택 매칭 안됨: {}", benefit.getSummary());
                }
            }

            if (!matchingBenefits.isEmpty()) {
                log.info("🎯 카드에 매칭된 혜택 수: {}", matchingBenefits.size());
                CardBenefitDTO cardInfo = CardBenefitDTO.builder()
                    .cardId(card.getId())
                    .cardName(card.getName())
                    .benefits(matchingBenefits)
                    .build();

                availableCards.add(cardInfo);
            } else {
                log.info("❌ 카드에 매칭된 혜택 없음: {}", card.getName());
            }
        }

        return availableCards;
    }


    private List<BenefitInfoDTO> createBenefitInfoList(Benefit benefit, ChannelType channelType) {
        List<BenefitInfoDTO> benefits = new ArrayList<>();

        // 할인 혜택 확인
        for (DiscountBenefit discount : benefit.getDiscountBenefits()) {
            if (channelType == null || discount.getChannel() == channelType || discount.getChannel() == ChannelType.BOTH) {
                benefits.add(BenefitInfoDTO.builder()
                    .benefitId(benefit.getId())
                    .benefitType("DISCOUNT")
                    .rate(discount.getRate())
                    .amount(discount.getAmount())
                    .build());
            }
        }

        // 포인트 혜택 확인
        for (PointBenefit point : benefit.getPointBenefits()) {
            if (channelType == null || point.getChannel() == channelType || point.getChannel() == ChannelType.BOTH) {
                benefits.add(BenefitInfoDTO.builder()
                    .benefitId(benefit.getId())
                    .benefitType("POINT")
                    .rate(point.getRate())
                    .build());
            }
        }

        // 캐시백 혜택 확인
        for (CashbackBenefit cashback : benefit.getCashbackBenefits()) {
            if (channelType == null || cashback.getChannel() == channelType || cashback.getChannel() == ChannelType.BOTH) {
                benefits.add(BenefitInfoDTO.builder()
                    .benefitId(benefit.getId())
                    .benefitType("CASHBACK")
                    .rate(cashback.getRate())
                    .amount(cashback.getAmount())
                    .build());
            }
        }

        return benefits;
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