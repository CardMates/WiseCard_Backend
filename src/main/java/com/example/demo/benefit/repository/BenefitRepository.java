package com.example.demo.benefit.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.benefit.entity.Benefit;
import com.example.demo.card.entity.CardBenefit;

import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT b FROM Benefit b " +
           "LEFT JOIN FETCH b.discountBenefits " +
           "LEFT JOIN FETCH b.pointBenefits " +
           "LEFT JOIN FETCH b.cashbackBenefits " +
           "WHERE :storeName MEMBER OF b.applicableTargets")
    List<Benefit> findByApplicableTargetsContaining(@Param("storeName") String storeName);


    @Query("SELECT b FROM Benefit b " +
           "JOIN CardBenefit cb ON b.id = cb.benefit.id " +
           "WHERE cb.card.id = :cardId AND :place MEMBER OF b.applicableTargets")
    List<Benefit> findByCardIdAndPlace(@Param("cardId") Long cardId, @Param("place") String place);

}
