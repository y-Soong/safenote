package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** T-A4 비밀번호 재발급 응답. */
@Getter
@Builder
public class AdminSessionPwdResponse {
    private String sessionCd;
    private String entryPwd;
    private String exitPwd;
}
