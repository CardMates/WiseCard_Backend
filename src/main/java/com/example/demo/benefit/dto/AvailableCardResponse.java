package com.example.demo.benefit.dto;

import com.example.demo.card.entity.Card;
import lombok.Builder;

import java.util.List;

@Builder
public record AvailableCardResponse(
        Long cardId,
        String cardName,
        Card.CardCompany cardCompany,
        String imgUrl,
        Card.CardType cardType,
        List<BenefitResponse> benefits, // 필터링을 통과한, 진짜 사용 가능한 혜택 목록
        PerformanceInfo performance,  // 실적 정보
        LimitInfo limits            // 한도 정보
) {

}
