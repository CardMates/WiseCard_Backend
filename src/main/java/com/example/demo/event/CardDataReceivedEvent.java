package com.example.demo.event;

import com.sub.grpc.CardData;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CardDataReceivedEvent {
    private CardData.CardBenefitList cardBenefitList;
    private LocalDateTime receivedAt;
    private String source; // 크롤링 서버 식별자
}

