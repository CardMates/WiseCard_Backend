package com.example.demo.event.CardData;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CardDataProcessedEvent {
    private int totalReceived;
    private int changedCount;
    private int newCount;
    private int unchangedCount;
    private LocalDateTime processedAt;
    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED
}



