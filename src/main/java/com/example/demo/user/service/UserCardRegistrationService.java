package com.example.demo.user.service;

import com.example.demo.benefit.dto.AvailableCardResponse;
import com.example.demo.benefit.dto.BenefitResponse;
import com.example.demo.benefit.entity.Benefit;
import com.example.demo.benefit.entity.CashbackBenefit;
import com.example.demo.benefit.entity.DiscountBenefit;
import com.example.demo.benefit.entity.PointBenefit;
import com.example.demo.benefit.util.ProtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.benefit.dto.CardWithBenefitResponse;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import com.example.demo.user.entity.UserCard;
import com.example.demo.user.repository.UserCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCardRegistrationService {
    private final UserCardRepository userCardRepository;
    private final CardRepository cardRepository;
    private final ProtoMapper protoMapper;

    // 사용자 카드 등록
    @Transactional
    public AvailableCardResponse registerCardToUser(Long userId, Long cardId) {

        // 카드 존재 확인
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("카드를 찾을 수 없습니다: " + cardId));

        // 이미 등록된 카드인지 확인
        if (userCardRepository.existsByUserIdAndCard_IdAndIsActiveTrue(userId, cardId)) {
            throw new RuntimeException("이미 등록된 카드입니다");
        }

        // UserCard 엔티티 생성
        UserCard userCard = UserCard.builder()
                .userId(userId)
                .card(card)
                .isActive(true)
                .registeredAt(LocalDateTime.now())
                .build();

        // 저장
        userCardRepository.save(userCard);

        List<BenefitResponse> benefitResponses = convertBenefitsToResponseDTOs(card.getBenefits());

        // 응답 DTO 생성
        return AvailableCardResponse.builder()
                .cardId(card.getId())
                .cardName(card.getName())
                .cardType(Card.CardType.valueOf(card.getType()))
                .cardCompany(card.getCardCompany())
                .imgUrl(card.getImgUrl())
                .benefits(benefitResponses)
                .build();
    }

    // 사용자 카드 등록 해제
    @Transactional
    public void unregisterCardFromUser(Long userId, Long cardId) {
        log.info("사용자 카드 등록 해제 요청 - 사용자: {}, 카드: {}", userId, cardId);

        UserCard userCard = userCardRepository.findByUserIdAndCardIdAndIsActiveTrue(userId, cardId)
                .orElseThrow(() -> new RuntimeException("등록된 카드를 찾을 수 없습니다"));

        userCard.setIsActive(false);
        userCardRepository.save(userCard);
        
        log.info("사용자 카드 등록 해제 완료 - 사용자: {}, 카드: {}", userId, cardId);
    }

    private List<BenefitResponse> convertBenefitsToResponseDTOs(List<Benefit> benefits) {
        List<BenefitResponse> benefitResponses = new ArrayList<>();

        for(Benefit benefit : benefits){
            for (DiscountBenefit db : benefit.getDiscountBenefits()){
                benefitResponses.add(createBenefitResponse(benefit, db, "DISCOUNT"));
            }
            for (PointBenefit pb : benefit.getPointBenefits()){
                benefitResponses.add(createBenefitResponse(benefit, pb, "POINT"));
            }
            for (CashbackBenefit cb : benefit.getCashbackBenefits()){
                benefitResponses.add(createBenefitResponse(benefit, cb, "CASHBACK"));
            }
        }
        return benefitResponses;
    }

    private BenefitResponse createBenefitResponse(Benefit parent, Object child, String type) {
        BenefitResponse.BenefitResponseBuilder builder = BenefitResponse.builder()
                .benefitId(parent.getId())
                .benefitType(type)
                .summary(parent.getSummary());

        if (child instanceof DiscountBenefit db) {
            builder.minimumSpending(db.getMinimumSpending())
                    .benefitLimit(db.getBenefitLimit())
                    .rate(db.getRate())
                    .amount(db.getAmount());
        } else if (child instanceof PointBenefit pb) {
            builder.minimumSpending(pb.getMinimumSpending())
                    .benefitLimit(pb.getBenefitLimit())
                    .rate(pb.getRate());
        } else if (child instanceof CashbackBenefit cb) {
            builder.minimumSpending(cb.getMinimumSpending())
                    .benefitLimit( cb.getBenefitLimit())
                    .rate(cb.getRate())
                    .amount(cb.getAmount());
        }
        return builder.build();
    }
}
