package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** R3 T4 개별 이수처리 응답. */
@Getter
@Builder
public class AdminCompletionResponse {
    private String attendanceCd;
    private String completionStatusCd;
}
