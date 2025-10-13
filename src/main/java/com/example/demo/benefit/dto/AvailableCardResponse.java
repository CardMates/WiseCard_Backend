package com.example.demo.benefit.dto;

import lombok.Builder;

@Builder
public record AvailableCardResponse( // CardWithBenefitResponse 에서 실적, 한도 필터링 한 응답
    Long cardId,
    String cardName,
    String imgUrl,
    String type,
    BenefitDetailDTO benefits,
    PerformanceInfo performance,  // 실적 정보
    LimitInfo limits            // 한도 정보
) {

}
