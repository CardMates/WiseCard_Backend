package com.example.demo.store.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.application.dto.ChannelType;
import com.example.demo.benefit.repository.BenefitRepository;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import com.example.demo.store.dto.OnlineStoreInfoDTO;
import com.example.demo.benefit.application.dto.CashbackBenefitDTO;
import com.example.demo.benefit.application.dto.DiscountBenefitDTO;
import com.example.demo.benefit.application.dto.PointBenefitDTO;
import com.example.demo.benefit.dto.AvailableCardResponse;
import com.example.demo.benefit.dto.BenefitDetailDTO;
import com.example.demo.benefit.dto.MatchingCardsResponse;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.store.dto.OnlineStoreSearchResponse;
import com.example.demo.store.dto.StoreInfoDTO;
import com.example.demo.user.entity.UserCard;
import com.example.demo.user.repository.UserCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineStoreService {

    private final UserCardRepository userCardRepository;
    private final KakaoMapService kakaoMapService;
    private final StoreCardMatchingService storeCardMatchingService;

    /**
     * 온라인 매장 조회
     */
    @Transactional(readOnly = true)
    public OnlineStoreSearchResponse searchOnlineStores(String category, Long userId) {
        log.info("온라인 매장 조회 시작 - 카테고리: {}, 사용자: {}", category, userId);
        
        // 1. 사용자 카드 목록 조회
        List<Card> userCards = getOnlineCards(userId);
        
        if (userCards.isEmpty()) {
            return OnlineStoreSearchResponse.builder()
                    .stores(new ArrayList<>())
                    .build();
        }
        
        // 2. 카카오 API로 온라인 매장 검색
        List<Map<String, Object>> kakaoStores = searchOnlineStoresFromKakao(category);
        
        // 3. 각 매장에 대해 온라인 카드 혜택 매칭
        List<StoreInfoDTO> storesWithCards = storeCardMatchingService.matchStoresWithCards(kakaoStores, userCards, ChannelType.ONLINE);
        
        // 4. StoreInfoDTO를 OnlineStoreInfoDTO로 변환
        List<OnlineStoreInfoDTO> onlineStores = convertToOnlineStoreDTOs(storesWithCards);
        
        log.info("온라인 매장 조회 완료 - 매장 수: {}", onlineStores.size());
        
        return OnlineStoreSearchResponse.builder()
                .stores(onlineStores)
                .build();
    }

    /**
     * 카카오 API로 온라인 매장 검색
     */
    private List<Map<String, Object>> searchOnlineStoresFromKakao(String category) {
        // 요청으로 온 카테고리를 그대로 카카오 API에 전달
        return kakaoMapService.searchPlacesByCategory(category);
    }

    /**
     * StoreInfoDTO를 OnlineStoreInfoDTO로 변환
     */
    private List<OnlineStoreInfoDTO> convertToOnlineStoreDTOs(List<StoreInfoDTO> storesWithCards) {
        return storesWithCards.stream()
                .map(store -> OnlineStoreInfoDTO.builder()
                        .storeId(store.id())
                        .storeName(store.placeName())
                        .availableCards(store.availableCards())
                        .build())
                .collect(Collectors.toList());
    }


    // 온라인 혜택 있는 카드 조회 
    private List<Card> getOnlineCards(Long userId) {
        List<UserCard> userCards = userCardRepository.findByUserIdAndIsActiveTrue(userId);
        return userCards.stream()
                .map(UserCard::getCard)
                .filter(this::hasOnlineBenefits)
                .collect(Collectors.toList());
    }

    // 카드가 온라인 혜택 가지는지 확인 
    private boolean hasOnlineBenefits(Card card) {
        return card.getBenefits().stream()
                .anyMatch(benefit -> 
                    benefit.getDiscountBenefits().stream().anyMatch(db -> 
                        db.getChannel() == ChannelType.ONLINE || db.getChannel() == ChannelType.BOTH) ||
                    benefit.getPointBenefits().stream().anyMatch(pb -> 
                        pb.getChannel() == ChannelType.ONLINE || pb.getChannel() == ChannelType.BOTH) ||
                    benefit.getCashbackBenefits().stream().anyMatch(cb -> 
                        cb.getChannel() == ChannelType.ONLINE || cb.getChannel() == ChannelType.BOTH)
                );
    }

    public MatchingCardsResponse getStoreCards(String storeId, Long userId) {
        List<Card> userCards = getOnlineCards(userId);

        if (userCards.isEmpty()) {
            return new MatchingCardsResponse(new ArrayList<>());
        }

        // 해당 매장에 적용되는 카드들만 필터링
        List<AvailableCardResponse> matchingCards = userCards.stream()
                .filter(card -> hasBenefitForStore(card, storeId))
                .map(card -> AvailableCardResponse.builder()
                        .cardId(card.getId())
                        .cardName(card.getName())
                        .imgUrl(card.getImgUrl())
                        .type(card.getType())
                        .benefits(convertBenefitsToDTO(card.getBenefits()))
                        .build())
                .collect(Collectors.toList());
        
        return new MatchingCardsResponse(matchingCards);
    }

    /**
     * 카드가 해당 매장에 혜택을 제공하는지 확인
     */
    private boolean hasBenefitForStore(Card card, String storeId) {
        return card.getBenefits().stream()
                .anyMatch(benefit -> benefit.getApplicableTargets().contains(storeId));
    }

    /**
     * Benefit 리스트를 BenefitDetailDTO로 변환
     */
    private BenefitDetailDTO convertBenefitsToDTO(List<Benefit> benefits) {
        List<DiscountBenefitDTO> discounts = new ArrayList<>();
        List<PointBenefitDTO> points = new ArrayList<>();
        List<CashbackBenefitDTO> cashbacks = new ArrayList<>();
        List<String> applicableCategory = new ArrayList<>();
        List<String> applicableTargets = new ArrayList<>();
        String summary = new String();
        
        for (Benefit benefit : benefits) {
            // 할인 혜택
            for (DiscountBenefit discount : benefit.getDiscountBenefits()) {
                discounts.add(DiscountBenefitDTO.builder()
                        .rate(discount.getRate())
                        .amount(discount.getAmount())
                        .minimumAmount(discount.getMinimumAmount())
                        .benefitLimit(discount.getBenefitLimit())
                        .channel(discount.getChannel())
                        .build());
            }
            
            // 포인트 혜택
            for (PointBenefit point : benefit.getPointBenefits()) {
                points.add(PointBenefitDTO.builder()
                        .rate(point.getRate())
                        .minimumAmount(point.getMinimumAmount())
                        .benefitLimit(point.getBenefitLimit())
                        .channel(point.getChannel())
                        .build());
            }
            
            // 캐시백 혜택
            for (CashbackBenefit cashback : benefit.getCashbackBenefits()) {
                cashbacks.add(CashbackBenefitDTO.builder()
                        .rate(cashback.getRate())
                        .amount(cashback.getAmount())
                        .minimumAmount(cashback.getMinimumAmount())
                        .benefitLimit(cashback.getBenefitLimit())
                        .channel(cashback.getChannel())
                        .build());
            }
            
            // 적용 카테고리 및 대상
            applicableCategory.addAll(benefit.getApplicableCategory());
            applicableTargets.addAll(benefit.getApplicableTargets());
            summary = benefit.getSummary();
        }
        
        return new BenefitDetailDTO(discounts, points, cashbacks, applicableCategory, applicableTargets, summary);
    }

}
