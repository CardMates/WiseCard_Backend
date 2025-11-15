package com.example.demo.user.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.user.entity.UserCard;

public interface UserCardRepository extends JpaRepository<UserCard, Long> {
    
    /**
     * 사용자 보유 카드 조회 (활성 상태만)
     */
    @Query("SELECT uc FROM UserCard uc WHERE uc.userId = :userId AND uc.isActive = true")
    List<UserCard> findByUserIdAndIsActiveTrue(@Param("userId") Long userId);
    
    /**
     * 특정 카드가 사용자에게 등록되어 있는지 확인
     */
    @Query("SELECT uc FROM UserCard uc WHERE uc.userId = :userId AND uc.card.id = :cardId AND uc.isActive = true")
    Optional<UserCard> findByUserIdAndCardIdAndIsActiveTrue(@Param("userId") Long userId, @Param("cardId") Long cardId);
    
    /**
     * 사용자 보유 카드 ID 목록 조회
     */
    @Query("SELECT uc.card.id FROM UserCard uc WHERE uc.userId = :userId AND uc.isActive = true")
    List<Long> findCardIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자와 카드의 등록 여부 확인
     */
    boolean existsByUserIdAndCard_IdAndIsActiveTrue(Long userId, Long cardId);

    List<Card> findByUserId(Long userId);
}
