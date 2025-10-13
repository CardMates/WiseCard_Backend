package com.example.demo.event.CardData;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.demo.event.CardData.service.CardDataChangeDetectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardDataEventListener {
    
    private final CardDataChangeDetectionService changeDetectionService;
    
    /**
     * 카드 데이터 수신 이벤트 처리 (1순위: 데이터 변경 감지)
     */
    @EventListener
    @Async
    @Order(1)
    public void handleCardDataReceived(CardDataReceivedEvent event) {
        log.info("카드 데이터 수신 이벤트 처리 시작 - 소스: {}, 수신시간: {}", 
                event.getSource(), event.getReceivedAt());
        
        try {
            // 실제 데이터 변경 감지 및 처리
            changeDetectionService.processCardDataChanges(event.getCardBenefitList());
            
            log.info("카드 데이터 변경 감지 및 처리 완료 - 소스: {}", event.getSource());
            
        } catch (Exception e) {
            log.error("카드 데이터 변경 감지 처리 실패 - 소스: {}", event.getSource(), e);
            // 비동기 처리이므로 예외를 다시 던지지 않음 (로그만 기록)
        }
    }
    
    /**
     * 카드 데이터 처리 완료 이벤트 처리 (2순위: 후처리)
     */
    @EventListener
    @Async
    @Order(2)
    public void handleCardDataProcessed(CardDataProcessedEvent event) {
        log.info("카드 데이터 처리 완료 이벤트 - 총: {}, 변경: {}, 신규: {}, 무변경: {}, 상태: {}", 
                event.getTotalReceived(), event.getChangedCount(), event.getNewCount(), 
                event.getUnchangedCount(), event.getStatus());
        
        try {
            // 처리 결과에 따른 추가 로직
            if ("SUCCESS".equals(event.getStatus())) {
                log.info("모든 카드 데이터가 성공적으로 처리되었습니다.");
                // 성공 시 추가 처리 (알림, 메트릭 등)
                handleSuccessfulProcessing(event);
            } else if ("PARTIAL_SUCCESS".equals(event.getStatus())) {
                log.warn("일부 카드 데이터 처리에 실패했습니다.");
                // 부분 성공 시 추가 처리 (경고 알림 등)
                handlePartialSuccessProcessing(event);
            } else {
                log.error("카드 데이터 처리에 실패했습니다.");
                // 실패 시 추가 처리 (에러 알림, 재시도 등)
                handleFailedProcessing(event);
            }
            
        } catch (Exception e) {
            log.error("카드 데이터 처리 완료 이벤트 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 성공적인 처리 완료 후 추가 로직
     */
    private void handleSuccessfulProcessing(CardDataProcessedEvent event) {
        // 예: 관리자에게 성공 알림, 메트릭 수집, 통계 업데이트 등
        log.info("성공 처리 후 추가 로직 실행 - 변경: {}, 신규: {}", 
                event.getChangedCount(), event.getNewCount());
    }
    
    /**
     * 부분 성공 처리 후 추가 로직
     */
    private void handlePartialSuccessProcessing(CardDataProcessedEvent event) {
        // 예: 관리자에게 경고 알림, 실패한 항목 재시도 등
        log.warn("부분 성공 처리 후 추가 로직 실행 - 총: {}, 성공: {}, 실패: {}", 
                event.getTotalReceived(), 
                event.getChangedCount() + event.getNewCount(),
                event.getTotalReceived() - event.getChangedCount() - event.getNewCount() - event.getUnchangedCount());
    }
    
    /**
     * 실패 처리 후 추가 로직
     */
    private void handleFailedProcessing(CardDataProcessedEvent event) {
        // 예: 관리자에게 에러 알림, 전체 재시도 등
        log.error("실패 처리 후 추가 로직 실행 - 총: {}", event.getTotalReceived());
    }
}