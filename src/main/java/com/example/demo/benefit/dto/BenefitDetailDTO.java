package com.example.demo.benefit.dto;

import com.example.demo.benefit.application.dto.DiscountBenefitDTO;
import com.example.demo.benefit.application.dto.PointBenefitDTO;
import com.example.demo.benefit.application.dto.CashbackBenefitDTO;

import java.util.List;

public record BenefitDetailDTO(
    List<DiscountBenefitDTO> discounts,
    List<PointBenefitDTO> points,
    List<CashbackBenefitDTO> cashbacks,
    List<String> applicableCategory,
    List<String> applicableTargets,
    String summary
) {}
