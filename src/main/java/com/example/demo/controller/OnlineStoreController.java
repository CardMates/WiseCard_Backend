package com.example.demo.controller;

import com.example.demo.auth.util.AuthUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.benefit.dto.MatchingCardsResponse;
import com.example.demo.benefit.service.OptimalBenefitService;
import com.example.demo.store.dto.OnlineStoreSearchResponse;
import com.example.demo.store.service.OnlineStoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/online-stores")
@RequiredArgsConstructor
@Slf4j
public class OnlineStoreController {

    private final OnlineStoreService onlineStoreService;
    private final OptimalBenefitService optimalBenefitService;

    /**
     * 온라인 매장 조회 API
     */
    @GetMapping("/search")
    public ResponseEntity<OnlineStoreSearchResponse> searchOnlineStores(
            @RequestParam(required = false) String category) {
            
            
        Long userId = AuthUtils.getMemberId();
        
        OnlineStoreSearchResponse response = onlineStoreService.searchOnlineStores(category, userId);
        
        
        return ResponseEntity.ok(response);
    
    }

    /**
     * 특정 온라인 매장의 카드 혜택 상세 조회 API
     */
    @GetMapping("/{storeName}/cards")
    public ResponseEntity<MatchingCardsResponse> getStoreCards(
            @PathVariable String storeName) {

        Long userId = AuthUtils.getMemberId();
        
        try {
            MatchingCardsResponse response = optimalBenefitService.getMatchingCardsWithRealTimeFilter(storeName, userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
