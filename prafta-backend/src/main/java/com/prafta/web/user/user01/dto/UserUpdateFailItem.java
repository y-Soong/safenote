package com.prafta.web.user.user01.dto;

public record UserUpdateFailItem(
        int index,          // 몇 번째 요청인지(0-based)
        String errorItem,
        String errorCode,
        String message
) {}