package com.example.demo.benefit.application.dto;

import lombok.Builder;

@Builder
public record CashbackBenefitDTO(
        double rate,
        double amount,
        long minimumAmount,
        long benefitLimit, 
        ChannelType channel,
        Integer minimumSpending
) {
}
