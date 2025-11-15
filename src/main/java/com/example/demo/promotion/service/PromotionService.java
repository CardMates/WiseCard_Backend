package com.example.demo.promotion.service;

import com.example.demo.auth.util.AuthUtils;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import com.example.demo.promotion.dto.ActivePromotionResponse;
import com.example.demo.promotion.entity.CardPromotion;
import com.example.demo.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final CardRepository cardRepository;


    // 사용자별 활성 프로모션 조회
    public List<ActivePromotionResponse> getActivePromotions() {

        Long userId = AuthUtils.getMemberId();
        List<Card> userCards = cardRepository.findByUserId(userId);

        List<Card.CardCompany> userCardCompanies = userCards.stream()
                .map(Card ::getCardCompany)
                .distinct()
                .toList();
        List<CardPromotion> cardPromotions = promotionRepository.findActivePromotionsByUserCardCompany(userCardCompanies, LocalDateTime.now());

        return cardPromotions.stream()
                .map(ActivePromotionResponse::of)
                .toList();
    }
}
