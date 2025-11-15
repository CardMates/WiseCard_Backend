package com.example.demo.card.entity;

import com.example.demo.benefit.entity.Benefit;
import com.sub.grpc.CardCompanyOuterClass;
import com.sub.grpc.CardData;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private CardCompany cardCompany;
    @Enumerated(EnumType.STRING)
    private CardType cardType;
    private String imgUrl;
    private String type;
    private String name;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CardBenefit> cardBenefits = new ArrayList<>();

    // 기존 코드와의 호환성을 위한 메서드
    public List<Benefit> getBenefits() {
        return cardBenefits.stream()
                .map(CardBenefit::getBenefit)
                .collect(java.util.stream.Collectors.toList());
    }

    @Column(name = "card_id", unique = true)
    private Integer cardId; // proto 의 card_id 와 매핑

    public enum CardCompany {
        HANA, HYUNDAI, KOOKMIN, LOTTE, SAMSUNG, SHINHAN
    }
    public enum CardType {
        CREDIT, DEBIT
    }

    @Builder
    public Card(Long id, CardCompany cardCompany, CardType cardType, String imgUrl, String type, List<CardBenefit> cardBenefits, Integer cardId, String name) {
        this.id = id;
        this.cardCompany = cardCompany;
        this.cardType = cardType;
        this.imgUrl = imgUrl;
        this.type = type;
        this.cardBenefits = cardBenefits != null ? cardBenefits : new ArrayList<>();
        this.cardId = cardId;
        this.name = name;
    }
}
