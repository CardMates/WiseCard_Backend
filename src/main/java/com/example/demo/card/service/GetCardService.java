package com.example.demo.card.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.benefit.dto.BenefitDetailDTO;
import com.example.demo.benefit.dto.CardWithBenefitResponse;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.util.BenefitConverter;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCardService {
    private final CardRepository cardRepository;
    private final BenefitConverter benefitConverter;

    public List<CardWithBenefitResponse> getCard() {
        return getCards(null, null, null);
    }

    public List<CardWithBenefitResponse> getCards(String cardBank, String cardType, String cardName) {
        List<Card> cards = cardRepository.findAllWithBenefits();

        return cards.stream()
                .filter(card -> {
                    // 카드사 필터링
                    if (cardBank != null && !cardBank.trim().isEmpty()) {
                        if (!card.getCardCompany().name().toLowerCase().contains(cardBank.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // 카드 타입 필터링
                    if (cardType != null && !cardType.trim().isEmpty()) {
                        if (!card.getCardType().name().toLowerCase().contains(cardType.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // 카드명 검색 (부분 일치, 대소문자 구분 없음)
                    if (cardName != null && !cardName.trim().isEmpty()) {
                        if (!card.getName().toLowerCase().contains(cardName.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(card -> CardWithBenefitResponse.builder()
                        .cardId(card.getId())
                        .cardName(card.getName())
                        .cardCompany(card.getCardCompany())
                        .imgUrl(card.getImgUrl())
                        .cardType(card.getCardType())
                        .benefits(convertToBenefitDetailDTO(card.getBenefits()))
                        .build())
                .collect(Collectors.toList());
    }

    private BenefitDetailDTO convertToBenefitDetailDTO(List<Benefit> benefits) {
        return benefitConverter.convertMultipleBenefitsToDTO(benefits);
    }
}
