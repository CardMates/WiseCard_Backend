package com.example.demo.benefit.application.dto;

import lombok.Builder;

@Builder
public record PointBenefitDTO(
        double rate,
        long minimumAmount,
        long benefitLimit,
        ChannelType channel,
        Integer minimumSpending,
        String name
){
}
