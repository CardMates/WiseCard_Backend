package com.example.demo.grpc;

import com.example.demo.event.Promotion.PromotionReceivedEvent;
import com.sub.grpc.CardPromotionServiceGrpc;
import com.sub.grpc.Promotion;
import com.sub.grpc.PromotionServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl extends CardPromotionServiceGrpc.CardPromotionServiceImplBase {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void savedPromotions(Promotion.CardPromotionList request, StreamObserver<Promotion.PromotionSaveResponse> responseObserver){
        try{
            PromotionReceivedEvent event = PromotionReceivedEvent.builder()
                    .cardPromotionList(request)
                    .receivedAt(LocalDateTime.now())
                    .source("crawler-server")
                    .build();
            applicationEventPublisher.publishEvent(event);
            responseObserver.onNext(Promotion.PromotionSaveResponse.newBuilder().build());
            responseObserver.onCompleted();

        }catch (Exception e){
            responseObserver.onError(e);
        }
    }

}
