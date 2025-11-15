package com.example.demo.card.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.card.entity.Card;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card,Long> {

    @Query("SELECT c FROM Card c JOIN UserCard uc ON c.id = uc.card.id WHERE uc.userId = :userId AND uc.isActive = true")
    List<Card> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Card c LEFT JOIN FETCH c.cardBenefits cb LEFT JOIN FETCH cb.benefit b")
    List<Card> findAllWithBenefits();

    Optional<Card> findByCardId(Integer cardId);
}
