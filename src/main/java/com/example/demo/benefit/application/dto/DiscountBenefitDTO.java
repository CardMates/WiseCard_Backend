package com.example.demo.benefit.application.dto;

import lombok.Builder;

@Builder
public record DiscountBenefitDTO(
        double rate,
        double amount,
        double minimumAmount,
        double benefitLimit,
        ChannelType channel,
        Integer minimumSpending
) {
}
