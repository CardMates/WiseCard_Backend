package com.example.demo.grpc;

import com.example.demo.event.CardData.CardDataReceivedEvent;
import com.sub.grpc.CardDataServiceGrpc;
import com.sub.grpc.CardData;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardDataServiceImpl extends CardDataServiceGrpc.CardDataServiceImplBase {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public void saveCardData(CardData.CardBenefitList request,
                             StreamObserver<CardData.CardSaveResponse> responseObserver) {
        
        log.info("=== gRPC CardDataServiceImpl.saveCardData 호출됨 ===");
        log.info("gRPC 요청 수신: {} 개의 카드 데이터", request.getCardBenefitsList().size());
        log.info("요청 객체: {}", request);
        
        // 디버깅을 위한 상세 로그 추가
        if (request.getCardBenefitsList().isEmpty()) {
            log.warn("⚠️ 빈 카드 데이터가 수신되었습니다! Postman에서 올바른 데이터를 보내고 있는지 확인하세요.");
        } else {
            log.info("수신된 카드 데이터 상세 정보:");
            for (int i = 0; i < request.getCardBenefitsList().size(); i++) {
                var cardBenefit = request.getCardBenefitsList().get(i);
                log.info("  카드 {}: ID={}, 이름={}, 회사={}, 혜택수={}", 
                    i+1, cardBenefit.getCardId(), cardBenefit.getCardName(), 
                    cardBenefit.getCardCompany(), cardBenefit.getBenefitsList().size());
            }
        }
        
        try {
            // 1. 이벤트 발행 (비동기 처리로 모든 로직 처리)
            CardDataReceivedEvent event = CardDataReceivedEvent.builder()
                    .cardBenefitList(request)
                    .receivedAt(LocalDateTime.now())
                    .source("crawler-server")
                    .build();
            
            eventPublisher.publishEvent(event);
            
            // 2. 즉시 성공 응답 반환 (실제 처리는 이벤트 리스너에서 비동기로)
            CardData.CardSaveResponse response = CardData.CardSaveResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("카드 데이터 처리 요청이 접수되었습니다. 비동기로 처리 중입니다.")
                    .setSavedCount(request.getCardBenefitsList().size())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.info("gRPC 응답 전송 완료: {} 개 처리 요청 접수", request.getCardBenefitsList().size());
            
        } catch (Exception e) {
            log.error("gRPC 서비스 처리 중 오류 발생", e);
            
            // 에러 응답 생성
            CardData.CardSaveResponse errorResponse = CardData.CardSaveResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("서버 오류: " + e.getMessage())
                    .setSavedCount(0)
                    .build();
            
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}
