package com.example.demo.card.service;

import com.example.demo.benefit.dto.AvailableCardResponse;
import com.example.demo.benefit.dto.BenefitResponse;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCardService {

    private final CardRepository cardRepository;

    public List<AvailableCardResponse> getCard() {
        return getCards(null, null, null);
    }

    public List<AvailableCardResponse> getCards(String cardBank, String cardType, String cardName) {
        
        // 1. 혜택 정보가 모두 포함된 카드 목록을 조회 (중복 가능성 있음)
        List<Card> cardsWithDuplicates = cardRepository.findAllWithBenefits();

        // 2. LinkedHashSet을 사용하여 카드의 중복을 제거 (순서는 유지됨)
        Set<Card> uniqueCards = new LinkedHashSet<>(cardsWithDuplicates);

        // 3. 중복이 제거된 카드 목록을 스트림으로 처리
        return uniqueCards.stream()
                .filter(card -> filterCard(card, cardBank, cardType, cardName))
                .map(this::convertToAvailableCardResponse) // DTO 변환
                .collect(Collectors.toList());
    }

    private boolean filterCard(Card card, String cardBank, String cardType, String cardName) {
        // (이전과 동일한 필터링 로직)
        if (cardBank != null && !cardBank.trim().isEmpty()) {
            if (!card.getCardCompany().name().toLowerCase().contains(cardBank.toLowerCase())) return false;
        }
        if (cardType != null && !cardType.trim().isEmpty()) {
            if (!card.getCardType().name().toLowerCase().contains(cardType.toLowerCase())) return false;
        }
        if (cardName != null && !cardName.trim().isEmpty()) {
            if (!card.getName().toLowerCase().contains(cardName.toLowerCase())) return false;
        }
        return true;
    }

    /**
     * Card 엔티티를 AvailableCardResponse DTO로 변환하는 메서드
     */
    private AvailableCardResponse convertToAvailableCardResponse(Card card) {
        // Fetch Join으로 모든 혜택 정보가 이미 로드되었으므로, card.getBenefits() 호출은 추가 쿼리를 발생시키지 않음
        List<BenefitResponse> benefitResponses = new ArrayList<>();
        for (Benefit benefit : card.getBenefits()) {
            addBenefitResponses(benefitResponses, benefit);
        }

        return AvailableCardResponse.builder()
                .cardId(card.getId())
                .cardName(card.getName())
                .cardCompany(card.getCardCompany())
                .imgUrl(card.getImgUrl())
                .cardType(card.getCardType())
                .benefits(benefitResponses)
                .performance(null)
                .build();
    }

    private void addBenefitResponses(List<BenefitResponse> benefitResponses, Benefit benefit) {
        for (DiscountBenefit discount : benefit.getDiscountBenefits()) {
            benefitResponses.add(createBenefitResponseDTO(benefit, discount, "DISCOUNT"));
        }
        for (PointBenefit point : benefit.getPointBenefits()) {
            benefitResponses.add(createBenefitResponseDTO(benefit, point, "POINT"));
        }
        for (CashbackBenefit cashback : benefit.getCashbackBenefits()) {
            benefitResponses.add(createBenefitResponseDTO(benefit, cashback, "CASHBACK"));
        }
    }

    private BenefitResponse createBenefitResponseDTO(Benefit parent, Object child, String type) {
        BenefitResponse.BenefitResponseBuilder builder = BenefitResponse.builder()
                .benefitId(parent.getId())
                .benefitType(type)
                .summary(parent.getSummary());

        if (child instanceof DiscountBenefit db) {
            builder.minimumSpending(db.getMinimumSpending())
                   .benefitLimit(db.getBenefitLimit())
                   .rate(db.getRate())
                   .amount(db.getAmount());
        } else if (child instanceof PointBenefit pb) {
            builder.minimumSpending(pb.getMinimumSpending())
                   .benefitLimit((long) pb.getBenefitLimit()) // 타입이 double일 수 있으므로 캐스팅
                   .rate(pb.getRate());
        } else if (child instanceof CashbackBenefit cb) {
            builder.minimumSpending(cb.getMinimumSpending())
                   .benefitLimit((long) cb.getBenefitLimit()) // 타입이 double일 수 있으므로 캐스팅
                   .rate(cb.getRate())
                   .amount(cb.getAmount());
        }
        return builder.build();
    }
}
