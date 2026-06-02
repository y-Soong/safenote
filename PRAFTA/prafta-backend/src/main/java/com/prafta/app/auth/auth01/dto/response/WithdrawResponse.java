package com.prafta.app.auth.auth01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-07: 회원 탈퇴 응답.
 */
@Getter
@Builder
public class WithdrawResponse {
    private final boolean success;
}
