package com.prafta.app.auth.auth01.dto.request;

import lombok.Data;

/**
 * prafta-app-010-07: 회원 탈퇴 요청.
 *
 * <p>식별자는 바디로 받지 않는다(JWT 출처). confirmed=true 게이트만 받는다.
 */
@Data
public class WithdrawRequest {
    private boolean confirmed;
}
