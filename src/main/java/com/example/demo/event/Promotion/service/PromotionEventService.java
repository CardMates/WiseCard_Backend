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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionEventService {

    private final ProtoMapper protoMapper;
    private final PromotionRepository promotionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // 데이터 변경 감지 없이 프로모션 엔티티 만들어서 저장하는 걸로
    @Transactional
    public PromotionProcessedEvent processPromotionData(PromotionReceivedEvent event) {

        int totalReceived = event.getCardPromotionList().getCardPromotionCount();
        int processedCount = 0;
        int failedCount = 0;

        for (Promotion.CardPromotion promotion : event.getCardPromotionList().getCardPromotionList()){
            try{
                processPromotion(promotion);
                processedCount++;
            }catch (Exception e){
                log.error("프로모션 처리 실패: {}", promotion.getDescription(), e);
                failedCount++;
            }
        }
        log.info("프로모션 처리 완료 - 성공: {}, 실패: {}", processedCount, failedCount);
        PromotionProcessedEvent response = createPromotionProcessedEvent(totalReceived, processedCount);
        applicationEventPublisher.publishEvent(response);

        return response;
    }

    // 개별 프로모션 생성
    private void processPromotion(Promotion.CardPromotion protoPromotion) {

        try {
            Card.CardCompany cardCompany = protoMapper.mapToCardCompany(protoPromotion.getCardCompany());

            CardPromotion promotion = CardPromotion.builder()
                    .cardCompany(cardCompany)
                    .description(protoPromotion.getDescription())
                    .imgUrl(protoPromotion.getImgUrl())
                    .url(protoPromotion.getUrl())
                    .build();

            promotionRepository.save(promotion);
            log.info("프로모션 저장 완료: {}", promotion.getDescription());
        } catch (Exception e) {
            log.error("프로모션 저장 실패: {}", protoPromotion.getDescription(), e);
            throw e;
        }
    }


    public PromotionProcessedEvent createPromotionProcessedEvent(int totalReceived, int processedCount) {
        return PromotionProcessedEvent.builder()
                .totalReceived(totalReceived)
                .processedCount(processedCount)
                .build();
    }


}
