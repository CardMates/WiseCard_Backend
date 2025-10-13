package com.example.demo.event.Promotion;

import com.sub.grpc.Promotion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PromotionReceivedEvent {
    private Promotion.CardPromotionList cardPromotionList;
    private LocalDateTime receivedAt;
    private String source; // 크롤링 서버 식별자
}
