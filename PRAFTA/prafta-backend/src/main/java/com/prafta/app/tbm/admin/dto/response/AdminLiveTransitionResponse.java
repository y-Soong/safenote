package com.prafta.app.tbm.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * R3 T1 교육 시작/종료 응답.
 *
 * <p>시작: statusCd='IN_PROGRESS', startedAt 채움(endedAt/autoCompletedCount 미사용).
 * <p>종료: statusCd='COMPLETED', endedAt 채움, autoCompletedCount=자동이수 처리 인원(T2).
 */
@Getter
@Builder
public class AdminLiveTransitionResponse {
    private String sessionCd;
    private String statusCd;
    private String startedAt;
    private String endedAt;
    private int autoCompletedCount;
}
