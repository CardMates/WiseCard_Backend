package com.example.demo.benefit.dto;

import lombok.Builder;

import java.util.List;

// 혜택 1개의 모든 정보를 담는 DTO
@Builder
public record BenefitResponse(
        Long benefitId,
        String benefitType, // "DISCOUNT", "POINT", "CASHBACK"
        String summary, // "CGV 20% 할인"

        // 혜택 상세 내용
        Double rate, // 할인율/적립률
        Double amount, // 할인/캐시백 금액

        // 혜택 조건
        Integer minimumSpending, // 이 혜택을 받기 위한 '전월 실적 조건'
        Long benefitLimit, // 이 혜택의 '월간 한도'
        Long minimumPurchaseAmount, // '최소 결제 금액' 조건

        // 적용 대상
        List<String> applicableTargets, // "CGV", "스타벅스" 등 특정 가맹점
        String applicableCategory, // "CAFE", "MOVIE" 등 업종

        // 사용자 맞춤 정보 (필터링 시 채워짐)
        Boolean isPerformanceMet, // 사용자가 실적을 만족했는지
        Long currentUsage, // 사용자가 이번 달에 이 혜택으로 사용한 금액
        Long remainingLimit // 남은 한도
) {}

