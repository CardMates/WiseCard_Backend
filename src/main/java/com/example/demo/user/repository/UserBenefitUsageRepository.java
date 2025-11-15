package com.example.demo.user.repository;

import com.example.demo.user.entity.UserBenefitUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;


public interface UserBenefitUsageRepository extends JpaRepository<UserBenefitUsage, Long> {
    

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT COALESCE(SUM(ubu.usedAmount), 0) FROM UserBenefitUsage ubu " +
           "WHERE ubu.member.id = :userId AND ubu.card.id = :cardId AND ubu.benefitType = :benefitType")
    Long findTotalUsedAmountByUserAndCardAndBenefitType(@Param("userId") Long userId, 
                                                       @Param("cardId") Long cardId, 
                                                       @Param("benefitType") String benefitType);

    /**
     * 지정된 기간 동안 특정 하위 혜택의 누적 사용 금액을 조회합니다.
     * (DB에서 직접 합산하여 단일 값으로 반환)
     */
    @Query("SELECT COALESCE(SUM(ubu.usedAmount), 0L) " +
            "FROM UserBenefitUsage ubu " +
            "WHERE ubu.member.id = :userId " +
            "  AND ubu.card.id = :cardId " +
            "  AND ubu.benefitDetailId = :benefitDetailId " +
            "  AND ubu.benefitType = :benefitType " +
            "  AND ubu.transactionDate BETWEEN :startDate AND :endDate")
    Long getUsageAmountInPeriod(
            @Param("userId") Long userId,
            @Param("cardId") Long cardId,
            @Param("benefitDetailId") Long benefitDetailId,
            @Param("benefitType") String benefitType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
