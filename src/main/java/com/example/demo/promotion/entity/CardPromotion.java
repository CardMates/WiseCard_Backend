package com.example.demo.promotion.entity;

import com.example.demo.card.entity.Card;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class CardPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Card.CardCompany cardCompany;

    private String description;
    private String imgUrl;
    private String url;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder
    public CardPromotion(Card.CardCompany cardCompany, String description, String imgUrl, String url, LocalDateTime startDate, LocalDateTime endDate) {
        this.cardCompany = cardCompany;
        this.description = description;
        this.imgUrl = imgUrl;
        this.url = url;
        this.startDate = startDate;
        this.endDate = endDate;
    }

}
