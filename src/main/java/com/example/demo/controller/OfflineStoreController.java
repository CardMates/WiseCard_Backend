package com.example.demo.controller;

import com.example.demo.auth.util.AuthUtils;
import com.example.demo.card.entity.Card;
import com.example.demo.card.repository.CardRepository;
import com.example.demo.store.dto.*;
import com.example.demo.store.service.KakaoMapService;
import com.example.demo.store.service.StoreCardMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simple-stores")
@RequiredArgsConstructor
@Slf4j
public class OfflineStoreController {
    
    private final CardRepository cardRepository;
    private final KakaoMapService kakaoMapService;
    private final StoreCardMatchingService storeCardMatchingService;
    
    /**
     * 위치와 카테고리 기반 매장 추천 (카드 혜택 포함)
     */
    @PostMapping("/search")
    public ResponseEntity<StoreSearchResponse> searchStoresWithCards(
            @RequestBody StoreSearchRequest request) {
        
        Long userId = AuthUtils.getMemberId();

        // 1. 사용자 카드 목록 조회
        List<Card> userCards = cardRepository.findByUserId(userId);

        if (userCards.isEmpty()) {
            return ResponseEntity.ok(new StoreSearchResponse(new ArrayList<>()));
        }
        
        // 2. 카카오 API로 장소 검색
        List<Map<String, Object>> stores = kakaoMapService.searchPlaces(request.cat(), request.lat(), request.lng());
        
        // 3. 각 매장에 대해 실제 카드 혜택 매칭
        List<StoreInfoDTO> storesWithCards = storeCardMatchingService.matchStoresWithCards(stores, userCards);
        
        return ResponseEntity.ok(new StoreSearchResponse(storesWithCards));
    }
}