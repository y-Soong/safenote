package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * T-A3 세션 개설/임시저장 응답.
 *
 * <p>개설(OPENED) 시 입실/종료 비밀번호와 위험성평가 0건 경고메시지를 포함한다.
 * 임시저장(DRAFT) 시 비밀번호는 null.
 */
@Getter
@Builder
public class AdminSessionSaveResponse {
    private String sessionCd;
    private String statusCd;
    private String entryPwd;            // OPENED 시에만
    private String exitPwd;             // OPENED 시에만
    private String warningMessage;      // 위험성평가 0건일 때만 채움
}
