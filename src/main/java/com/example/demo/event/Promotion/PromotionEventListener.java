package com.example.demo.event.Promotion;

import com.example.demo.event.Promotion.service.PromotionEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEventListener {

    private final PromotionEventService promotionEventService;

    public void handlePromotionReceived(PromotionReceivedEvent event){
        log.info("프로모션 데이터 수신 이벤트 처리 : {}", event);
        promotionEventService.processPromotionData(event);
    }
}
