package com.prafta.app.tbm.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * R3 T4 개별 이수처리 요청. sessionCd/attendanceCd 는 path 에서 받는다.
 *
 * <p>completionStatusCd ∈ {COMPLETED, NOT_COMPLETED}. NOT_COMPLETED 시 reason 10자 이상 필수(서버 검증).
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminCompletionRequest {
    private String completionStatusCd;
    private String reason;
}
