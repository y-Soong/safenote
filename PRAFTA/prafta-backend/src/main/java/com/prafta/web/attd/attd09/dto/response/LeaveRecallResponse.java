package com.prafta.web.attd.attd09.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 관리자 수동 부여 연차 회수(soft cancel) 응답 (PRAFTA-031).
 * POST /attd09/leave-grant/{grantId}/recall.
 *
 * <p>회수 성공 시 회수된 부여 ID와 전환된 상태(CANCELED)를 반환한다.
 */
@Value
@Builder
public class LeaveRecallResponse {

    /** 회수된 부여 ID */
    String grantId;

    /** 전환된 상태 ([SYS040], 정상 회수 시 CANCELED) */
    String status;
}
