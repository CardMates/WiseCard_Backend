package com.example.demo.user.entity;

import java.time.LocalDateTime;

import com.example.demo.auth.entity.Member;
import com.example.demo.card.entity.Card;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_benefit_usage")
@Getter
@NoArgsConstructor
public class UserBenefitUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    // 어떤 하위 혜택(Discount, Point 등)을 사용했는지 식별하기 위한 ID
    @Column(nullable = false)
    private Long benefitDetailId;

    // 혜택의 종류를 구분 ("DISCOUNT", "POINT", "CASHBACK")
    @Column(nullable = false)
    private String benefitType;

    @Column(nullable = false)
    private Long usedAmount; // 사용한 혜택 금액

    @Column(nullable = false)
    private Long remainingLimit; // 남은 혜택 한도

    @Column(nullable = false)
    private String place; // 사용 장소

    // 혜택을 사용한 거래 일시
    @Column(nullable = false)
    private LocalDateTime transactionDate;


    @Builder
    public UserBenefitUsage(Member member, Card card, Long benefitDetailId, String benefitType, Long usedAmount, Long remainingLimit, String place, LocalDateTime usedAt) {
        this.member = member;
        this.card = card;
        this.benefitDetailId = benefitDetailId;
        this.benefitType = benefitType;
        this.usedAmount = usedAmount;
        this.remainingLimit = remainingLimit;
        this.place = place;
        this.transactionDate = usedAt;

    }
}

