package com.example.demo.store.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record StoreInfoDTO(
    String id,
    String placeName,
    List<CardBenefitDTO> availableCards,
    Double lat,
    Double lng
) {}