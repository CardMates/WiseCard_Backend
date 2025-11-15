package com.example.demo.benefit.util;

import com.example.demo.card.entity.Card;
import com.sub.grpc.CardCompanyOuterClass;
import com.sub.grpc.CardData;
import org.springframework.stereotype.Component;

@Component
public class ProtoMapper {
    public Card.CardType mapToCardType(CardData.CardType protoType) {
        return switch (protoType){
            case CREDIT -> Card.CardType.CREDIT;
            case DEBIT -> Card.CardType.DEBIT;
            default -> throw new IllegalArgumentException("Invalid card type");
        };
    }
    public Card.CardCompany mapToCardCompany(CardCompanyOuterClass.CardCompany protoCompany){
        return switch (protoCompany){
            case HANA -> Card.CardCompany.HANA;
            case HYUNDAI -> Card.CardCompany.HYUNDAI;
            case KOOKMIN -> Card.CardCompany.KOOKMIN;
            case LOTTE -> Card.CardCompany.LOTTE;
            case SAMSUNG -> Card.CardCompany.SAMSUNG;
            case SHINHAN -> Card.CardCompany.SHINHAN;
            default -> throw new IllegalArgumentException("Invalid card company");
        };
    }
}
