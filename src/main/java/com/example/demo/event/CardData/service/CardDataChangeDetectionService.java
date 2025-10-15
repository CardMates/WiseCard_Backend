package com.example.demo.event.CardData.service;

import java.util.*;

import com.example.demo.benefit.application.dto.ChannelType;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.benefit.repository.CashbackBenefitRepository;
import com.example.demo.benefit.repository.DiscountBenefitRepository;
import com.example.demo.benefit.repository.PointBenefitRepository;
import com.example.demo.benefit.util.ProtoMapper;
import com.example.demo.card.entity.CardBenefit;
import com.example.demo.card.repository.CardBenefitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.repository.BenefitRepository;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import com.sub.grpc.CardData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardDataChangeDetectionService {

    private final CardBenefitRepository cardBenefitRepository;
    private final BenefitRepository benefitRepository;
    private final CardRepository cardRepository;
    private final ProtoMapper protoMapper;
    private final DiscountBenefitRepository discountBenefitRepository;
    private final PointBenefitRepository pointBenefitRepository;
    private final CashbackBenefitRepository cashbackBenefitRepository;

    /**
     * 크롤링된 카드 데이터 처리 및 변경 감지
     */
    @Transactional
    public void processCardDataChanges(CardData.CardBenefitList crawledData) {
        log.info("크롤링된 카드 데이터 처리 시작");
        
        try {
            // gRPC 데이터 타입 확인 및 처리
            processCardBenefitList(crawledData);

        } catch (Exception e) {
            log.error("크롤링된 카드 데이터 처리 중 오류 발생", e);
        }
        
        log.info("크롤링된 카드 데이터 처리 완료");
    }

    /**
     * CardBenefitList 데이터 처리
     */
    @Transactional
    public void processCardBenefitList(CardData.CardBenefitList cardBenefitList) {
        int processedCount = 0;
        int failedCount = 0;

        for (CardData.CardBenefit cardBenefit : cardBenefitList.getCardBenefitsList()) {
            try {
                log.info("처리 중인 카드: ID={}, 이름={}, 회사={}", 
                    cardBenefit.getCardId(), cardBenefit.getCardName(), cardBenefit.getCardCompany());

                // 카드 생성
                Card card = createCard(cardBenefit);

                for (CardData.Benefit protoBenefit : cardBenefit.getBenefitsList()) {
                    // Benefit 엔티티 새로 생성
                    Benefit benefit = createBenefit(protoBenefit, card);
                    CardBenefit newCardBenefit = CardBenefit.builder()
                            .card(card) // 기존 카드 엔티티
                            .benefit(benefit) // 새로 생성한 Benefit 엔티티
                            .build();

                    cardBenefitRepository.save(newCardBenefit);
                }
                processedCount++;
                log.info("카드 동기화 성공: 카드사 {}, 이름 {}", cardBenefit.getCardCompany(), cardBenefit.getCardName());


            } catch (Exception e) {
                failedCount++;
                log.error("카드 동기화 실패: 카드사 {}, 이름 {}", cardBenefit.getCardCompany(),cardBenefit.getCardName(), e);
            }
        }
        log.info("카드 동기화 완료: 성공 {}, 실패 {}", processedCount, failedCount);

    }

    private Card updateCardIfNeeded(Card card, CardData.CardBenefit cardBenefit) {
        boolean changed = false;

        if (!cardBenefit.getCardName().equals(card.getName())){
            card = card.builder()
                    .id(card.getId())
                    .cardId(card.getCardId())
                    .name(cardBenefit.getCardName())
                    .cardCompany(card.getCardCompany())
                    .cardType(card.getCardType())
                    .imgUrl(card.getImgUrl())
                    .type(card.getType())
                    .cardBenefits(card.getCardBenefits())
                    .build();
            changed = true;
        }

        if (!cardBenefit.getImgUrl().equals(card.getImgUrl())) {
            card = card.builder()
                    .id(card.getId())
                    .cardId(card.getCardId())
                    .name(card.getName())
                    .cardCompany(card.getCardCompany())
                    .cardType(card.getCardType())
                    .imgUrl(cardBenefit.getImgUrl())
                    .type(card.getType())
                    .cardBenefits(card.getCardBenefits())
                    .build();
            changed = true;
        }

        Card.CardCompany cardCompany = protoMapper.mapToCardCompany(cardBenefit.getCardCompany());

        if (!cardCompany.equals(card.getCardCompany())) {
            card = card.builder()
                    .id(card.getId())
                    .cardId(card.getCardId())
                    .name(card.getName())
                    .cardCompany(cardCompany)
                    .cardType(card.getCardType())
                    .imgUrl(card.getImgUrl())
                    .type(card.getType())
                    .cardBenefits(card.getCardBenefits())
                    .build();
            changed = true;
        }

        Card.CardType cardType = protoMapper.mapToCardType(cardBenefit.getCardType());

        if (!cardType.equals(card.getCardType())) {
            card = card.builder()
                    .id(card.getId())
                    .cardId(card.getCardId())
                    .name(card.getName())
                    .cardCompany(card.getCardCompany())
                    .cardType(cardType)
                    .imgUrl(card.getImgUrl())
                    .type(card.getType())
                    .cardBenefits(card.getCardBenefits())
                    .build();
            changed = true;
        }

        if (changed) {
            return cardRepository.save(card);
        }
        return card;
    }

    private Card createCard(CardData.CardBenefit cardBenefit) {

        Card.CardCompany cardCompany = protoMapper.mapToCardCompany(cardBenefit.getCardCompany());
        Card.CardType cardType = protoMapper.mapToCardType(cardBenefit.getCardType());

        Card card = Card.builder()
                .cardId(cardBenefit.getCardId())  // cardId 추가
                .name(cardBenefit.getCardName())
                .cardCompany(cardCompany)
                .cardType(cardType)
                .imgUrl(cardBenefit.getImgUrl())
                .build();

        return cardRepository.save(card);
    }

    private boolean syncCardBenefits(Card card, List<CardData.Benefit> benefitList) {
        boolean hasChanges = false;

        List<CardBenefit> existingCardBenefits = cardBenefitRepository.findByCard(card);

        if (!existingCardBenefits.isEmpty()) {
            cardBenefitRepository.deleteAll(existingCardBenefits);
            hasChanges = true;
        }

        for (CardData.Benefit protoBenefit : benefitList) {
            // Benefit 엔티티 새로 생성
            Benefit benefit = createBenefit(protoBenefit, card);
            CardBenefit newCardBenefit = CardBenefit.builder()
                    .card(card) // 기존 카드 엔티티
                    .benefit(benefit) // 새로 생성한 Benefit 엔티티
                    .build();
            cardBenefitRepository.save(newCardBenefit);
            hasChanges = true;
        }
        return hasChanges;

    }


    private Benefit createBenefit(CardData.Benefit protoBenefit, Card card) {
        Benefit benefit = Benefit.builder()
                .summary(protoBenefit.getSummary())
                .applicableCategory(new ArrayList<>(protoBenefit.getCategoriesList()))
                .applicableTargets(new ArrayList<>(protoBenefit.getTargetsList()))
                .card(card)
                .build();
        Benefit savedBenefit = benefitRepository.save(benefit);

        createDiscountBenefits(savedBenefit, protoBenefit.getDiscountsList());
        createPointBenefits(savedBenefit, protoBenefit.getPointsList());
        createCashbackBenefits(savedBenefit, protoBenefit.getCashbacksList());

        return savedBenefit;
    }
    private void createDiscountBenefits(Benefit benefit, List<CardData.DiscountBenefit> protoDiscounts){
        for (CardData.DiscountBenefit proto : protoDiscounts){
            DiscountBenefit discountBenefit = DiscountBenefit.builder()
                    .benefit(benefit)
                    .rate(proto.getRate())
                    .amount(proto.getAmount())
                    .minimumAmount(proto.getMinimumAmount())
                    .benefitLimit(proto.getBenefitLimit())
                    .channel(ChannelType.valueOf(proto.getChannel().name()))
                    .build();
            discountBenefitRepository.save(discountBenefit);
        }
    }
    private void createPointBenefits(Benefit benefit, List<CardData.PointBenefit> protoPoints){
        for (CardData.PointBenefit proto : protoPoints){
            PointBenefit pointBenefit = PointBenefit.builder()
                    .benefit(benefit)
                    .rate(proto.getRate())
                    .minimumAmount(proto.getMinimumAmount())
                    .benefitLimit(proto.getBenefitLimit())
                    .channel(ChannelType.valueOf(proto.getChannel().name()))
                    .build();
            pointBenefitRepository.save(pointBenefit);
        }
    }
    private void createCashbackBenefits(Benefit benefit, List<CardData.CashbackBenefit> protoCashbacks){
        for (CardData.CashbackBenefit proto : protoCashbacks){
            CashbackBenefit cashbackBenefit = CashbackBenefit.builder()
                    .benefit(benefit)
                    .rate(proto.getRate())
                    .amount(proto.getAmount())
                    .minimumAmount(proto.getMinimumAmount())
                    .benefitLimit(proto.getBenefitLimit())
                    .channel(ChannelType.valueOf(proto.getChannel().name()))
                    .build();
            cashbackBenefitRepository.save(cashbackBenefit);
        }
    }
}

