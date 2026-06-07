package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/** R3 T3 강제 퇴실 응답. completionStatusCd 는 자동 미이수('NOT_COMPLETED'). */
@Getter
@Builder
public class AdminForceExitResponse {
    private String attendanceCd;
    private String exitAt;
    private String completionStatusCd;
}
