package com.example.demo.card.service;

import java.util.List;

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
public class UserCardService {
    private final CardRepository cardRepository;
    private final BenefitConverter benefitConverter;

    /**
     * 사용자별 카드 조회 (필터링 및 검색 옵션 포함)
     */
    public List<CardWithBenefitResponse> getUserCards(Long userId, String cardBank, String cardType, String cardName) {
        List<Card> userCards = cardRepository.findByUserId(userId);

        return userCards.stream()
                .filter(card -> {
                    // 카드사 필터링
                    if (cardBank != null && !cardBank.trim().isEmpty()) {
                        if (!card.getCardCompany().name().equalsIgnoreCase(cardBank)) {
                            return false;
                        }
                    }
                    
                    // 카드 타입 필터링
                    if (cardType != null && !cardType.trim().isEmpty()) {
                        if (!card.getType().equalsIgnoreCase(cardType)) {
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
                .toList();
    }

    private BenefitDetailDTO convertToBenefitDetailDTO(List<Benefit> benefits) {
        return benefitConverter.convertMultipleBenefitsToDTO(benefits);
    }
}
