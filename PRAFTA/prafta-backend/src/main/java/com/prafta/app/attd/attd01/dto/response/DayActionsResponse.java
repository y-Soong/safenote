package com.prafta.app.attd.attd01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-002: 오늘/일상세 카드 액션 활성도 (계약 §3.1 actions).
 * <p>전부 서버 산출. 프론트는 표시만 한다.
 */
@Getter
@Builder
public class DayActionsResponse {
    private final boolean canRequestModify; // 수정요청
    private final boolean canCheckOut;      // 퇴근하기
    private final boolean canCheckIn;       // 2구간 재출근
}
