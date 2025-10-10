package com.example.demo.expense.dto;

public record PushNotificationRequest(
        // TODO: 유저 식별 위해 accessToken 추가
    String packageName,
    Long postedAt,
    String text,
    String title
) {}

