package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * R3 T3 강제 퇴실 요청. sessionCd/attendanceCd 는 path 에서 받는다.
 *
 * <p>reason(사유)은 nullable. 공백/null 허용(비워도 제출 가능, 빈문자는 서비스에서 NULL 정규화).
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminForceExitRequest {
    private String reason;
}
