package com.example.demo.promotion.dto;

import com.example.demo.card.entity.Card;
import com.example.demo.promotion.entity.CardPromotion;
import lombok.Builder;

@Builder
public record ActivePromotionResponse(
        Long id,
        Card.CardCompany cardCompany,
        String description,
        String imgUrl,
        String url
) {
    public static ActivePromotionResponse of(CardPromotion cardPromotion) {
        return ActivePromotionResponse.builder()
                .id(cardPromotion.getId())
                .cardCompany(cardPromotion.getCardCompany())
                .description(cardPromotion.getDescription())
                .imgUrl(cardPromotion.getImgUrl())
                .url(cardPromotion.getUrl())
                .build();
    }
}
