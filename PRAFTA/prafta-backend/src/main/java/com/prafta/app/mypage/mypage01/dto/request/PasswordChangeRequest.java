package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * prafta-app-010-04: 비밀번호 변경 요청.
 */
@Data
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
}
