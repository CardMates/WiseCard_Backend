package com.example.demo.store.dto;

import lombok.Builder;

@Builder
public record BenefitInfoDTO(
    Long benefitId,
    String benefitType,
    Double rate,
    Double amount,

    String summary,
    Long remainingLimit
) {}