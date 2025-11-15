package com.example.demo.promotion.repository;

import com.example.demo.card.entity.Card;
import com.example.demo.promotion.entity.CardPromotion;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<CardPromotion, Long> {

    // 사용자 카드사별 활성 프로모션 조회
    @Query("SELECT p FROM CardPromotion p WHERE p.cardCompany IN :cardCompanies " +
            "AND p.startDate <= :now AND p.endDate >= :now")
    List<CardPromotion> findActivePromotionsByUserCardCompany(
            @Param("cardCompanies") List<Card.CardCompany> cardCompanies,
            @Param("now") LocalDateTime now);
}
