package com.prafta.common.cmm.leave.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 관리자 수동 부여 연차 회수(soft cancel) 결과(attd09, PRAFTA-031).
 *
 * <p>회수 성공 시 회수된 부여 ID와 전환된 상태(CANCELED)를 담는다.
 */
@Getter
@Builder
public class LeaveRecallResultVO {

    /** 회수된 부여 ID */
    private final String grantId;

    /** 전환된 상태 ([SYS040], 정상 회수 시 CANCELED) */
    private final String status;
}
