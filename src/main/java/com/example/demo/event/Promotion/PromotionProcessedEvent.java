package com.example.demo.event.Promotion;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PromotionProcessedEvent {
    private int totalReceived;
    private int processedCount;
    private LocalDateTime processedAt;
    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED

}
