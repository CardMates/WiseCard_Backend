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

    @OneToMany(mappedBy = "cardId", cascade = CascadeType.ALL)
    private List<Benefit> benefits = new ArrayList<>();


    public enum CardCompany {
        HANA, HYUNDAI, KOOKMIN, LOTTE, SAMSUNG, SHINHAN
    }
    public enum CardType {
        CREDIT, DEBIT
    }

    @Builder
    public Card(Long id, CardCompany cardCompany, CardType cardType, String imgUrl, String type, List<Benefit> benefits) {
        this.id = id;
        this.cardCompany = cardCompany;
        this.cardType = cardType;
        this.imgUrl = imgUrl;
        this.type = type;
        this.benefits = benefits != null ? benefits : new ArrayList<>();
    }
}
