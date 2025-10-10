package com.example.demo.controller;

import com.example.demo.auth.util.AuthUtils;
import com.example.demo.benefit.dto.MatchingCardsResponse;
import com.example.demo.benefit.service.OptimalBenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/benefits")
@RequiredArgsConstructor
@Slf4j
public class BenefitController {
    
    private final OptimalBenefitService optimalBenefitService;
    
    /**
     * 매장별 매칭 카드 조회 (실시간 데이터)
     * 사용자 보유 카드 중 실적 달성 + 한도 여유 있는 카드만 반환
     *
     * 실시간 필터링:
     * 1. 실적 검증: 목표 실적을 달성한 카드만
     * 2. 한도 검증: 혜택 한도 내에서 사용 가능한 카드만
     */
    @GetMapping("/matching")
    public ResponseEntity<MatchingCardsResponse> getMatchingCards(
            @RequestParam String storeName) {

        Long userId = AuthUtils.getMemberId();

        try {
            MatchingCardsResponse response = optimalBenefitService.getMatchingCardsWithRealTimeFilter(
                    storeName, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("매칭 카드 조회 실패", e);
            return ResponseEntity.badRequest().body(
                    new MatchingCardsResponse(List.of()));
        }
    }
}
