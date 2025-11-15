package com.example.demo.benefit.dto;

import com.example.demo.card.entity.Card;
import lombok.Builder;

import java.util.List;

@Builder
public record CardWithBenefitResponse(
    Long cardId,
    String cardName,
    String imgUrl,
    List<BenefitResponse> benefits, // 개별 혜택 리스트
    Card.CardCompany cardCompany,
    Card.CardType cardType
) {}
