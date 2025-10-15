package com.example.demo.benefit.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.benefit.application.dto.CashbackBenefitDTO;
import com.example.demo.benefit.application.dto.DiscountBenefitDTO;
import com.example.demo.benefit.application.dto.PointBenefitDTO;
import com.example.demo.benefit.dto.BenefitDetailDTO;
import com.example.demo.benefit.entity.Benefit;

@Component
public class BenefitConverter {

    /**
     * 여러 Benefit을 하나의 BenefitDetailDTO로 변환
     * 카드별로 모든 Benefit의 정보를 합쳐서 반환
     */
    public BenefitDetailDTO convertMultipleBenefitsToDTO(List<Benefit> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return new BenefitDetailDTO(List.of(), List.of(), List.of(), List.of(), List.of(), null);
        }

        // 모든 Benefit의 정보를 합쳐서 하나의 BenefitDetailDTO로 변환 -> 실적 만족 여부는?
        List<DiscountBenefitDTO> allDiscounts = benefits.stream()
                .flatMap(benefit -> benefit.getDiscountBenefits().stream())
                .map(discount -> DiscountBenefitDTO.builder()
                        .rate(discount.getRate())
                        .amount(discount.getAmount())
                        .minimumAmount((long) discount.getMinimumAmount())
                        .benefitLimit((long) discount.getBenefitLimit())
                        .build())
                .collect(Collectors.toList());

        List<PointBenefitDTO> allPoints = benefits.stream()
                .flatMap(benefit -> benefit.getPointBenefits().stream())
                .map(point -> PointBenefitDTO.builder()
                        .rate(point.getRate())
                        .minimumAmount(point.getMinimumAmount())
                        .benefitLimit(point.getBenefitLimit())
                        .build())
                .collect(Collectors.toList());

        List<CashbackBenefitDTO> allCashbacks = benefits.stream()
                .flatMap(benefit -> benefit.getCashbackBenefits().stream())
                .map(cashback -> CashbackBenefitDTO.builder()
                        .rate(cashback.getRate())
                        .amount(cashback.getAmount())
                        .minimumAmount(cashback.getMinimumAmount())
                        .benefitLimit(cashback.getBenefitLimit())
                        .build())
                .collect(Collectors.toList());

        List<String> allApplicableCategories = benefits.stream()
                .flatMap(benefit -> benefit.getApplicableCategory() != null ?
                        benefit.getApplicableCategory().stream() : List.<String>of().stream())
                .distinct()
                .collect(Collectors.toList());

        List<String> allApplicableTargets = benefits.stream()
                .flatMap(benefit -> benefit.getApplicableTargets() != null ? 
                        benefit.getApplicableTargets().stream() : List.<String>of().stream())
                .distinct()
                .collect(Collectors.toList());

        String allSummaries = benefits.stream()
                .map(Benefit::getSummary)
                .filter(summary -> summary !=null && !summary.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));

        return new BenefitDetailDTO(allDiscounts, allPoints, allCashbacks, allApplicableCategories, allApplicableTargets, allSummaries);
    }


}
