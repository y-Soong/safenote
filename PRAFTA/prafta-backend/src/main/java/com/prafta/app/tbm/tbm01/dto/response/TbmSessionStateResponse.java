package com.prafta.app.tbm.tbm01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm-A5: 세션 상태 조회 응답.
 *
 * <p>관리자 시작/종료 판정 = statusCd(STATUS_CD). 클라이언트는 IN_PROGRESS=시작, COMPLETED=종료로 분기.
 * <p>syncStateCd 는 슬라이드 동기화 상태(참고 필드, 시작/종료 판정 기준 아님).
 */
@Getter
@Builder
public class TbmSessionStateResponse {
    private final String statusCd;       // SYS046
    private final String startedAt;
    private final String endedAt;
    private final String syncStateCd;    // SYS049 (참고)
}
