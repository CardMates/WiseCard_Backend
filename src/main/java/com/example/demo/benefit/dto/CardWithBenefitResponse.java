package com.example.demo.benefit.dto;

import com.example.demo.card.entity.Card;
import lombok.Builder;

@Builder
public record CardWithBenefitResponse(
    Long cardId,
    String cardName,
    String imgUrl,
    BenefitDetailDTO benefits,
    Card.CardCompany cardCompany,
    Card.CardType cardType
) {}
