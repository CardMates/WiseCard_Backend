package com.example.demo.event.Promotion.service;

import com.example.demo.benefit.util.ProtoMapper;
import com.example.demo.card.entity.Card;
import com.example.demo.event.Promotion.PromotionProcessedEvent;
import com.example.demo.event.Promotion.PromotionReceivedEvent;
import com.example.demo.promotion.entity.CardPromotion;
import com.example.demo.promotion.repository.PromotionRepository;
import com.sub.grpc.Promotion;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromotionEventService {

    private final ProtoMapper protoMapper;
    private final PromotionRepository promotionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // 데이터 변경 감지 없이 프로모션 엔티티 만들어서 저장하는 걸로
    @Transactional
    public PromotionProcessedEvent processPromotionData(PromotionReceivedEvent event) {

        int totalReceived = event.getCardPromotionList().getCardPromotionCount();
        int processedCount = 0;

        try{
            for (Promotion.CardPromotion promotion : event.getCardPromotionList().getCardPromotionList()) {
                processPromotion(promotion);
            }
        } catch (Exception e) {

        }
        PromotionProcessedEvent response = createPromotionProcessedEvent(totalReceived, processedCount);
        applicationEventPublisher.publishEvent(response);

        return response;
    }

    // 개별 프로모션 생성
    private void processPromotion(Promotion.CardPromotion protoPromotion) {

        Card.CardCompany cardCompany = protoMapper.mapToCardCompany(protoPromotion.getCardCompany());

        CardPromotion promotion = CardPromotion.builder()
                .cardCompany(cardCompany)
                .description(protoPromotion.getDescription())
                .imgUrl(protoPromotion.getImgUrl())
                .url(protoPromotion.getUrl())
                .build();

        promotionRepository.save(promotion);
    }


    public PromotionProcessedEvent createPromotionProcessedEvent(int totalReceived, int processedCount) {
        return PromotionProcessedEvent.builder()
                .totalReceived(totalReceived)
                .processedCount(processedCount)
                .build();
    }


}
