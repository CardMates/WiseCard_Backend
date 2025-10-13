package com.example.demo.controller;

import com.example.demo.promotion.dto.ActivePromotionResponse;
import com.example.demo.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/active")
    public List<ActivePromotionResponse> getActivePromotions() {
        return promotionService.getActivePromotions();
    }
}
