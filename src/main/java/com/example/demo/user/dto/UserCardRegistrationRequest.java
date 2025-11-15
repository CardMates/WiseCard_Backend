package com.example.demo.user.dto;

import lombok.Builder;

@Builder
public record UserCardRegistrationRequest(
    Long cardId     // 카드 ID
) {}

