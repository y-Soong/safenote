package com.prafta.app.tbm.tbm01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-004-C2: TBM 종료 응답.
 */
@Getter
@Builder
public class TbmExitResponse {
    private final String attendanceCd;
    private final String exitAt;               // yyyy-MM-dd HH:mm:ss
    private final String completionStatusCd;   // SYS053 COMPLETED
}
