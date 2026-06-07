package com.prafta.app.tbm.tbm01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm-A8/A9: 시작전 퇴실/중도퇴실 액션 응답(멱등).
 *
 * <p>success: 처리/멱등 성공 여부(항상 true 반환, 이미 처리된 경우도 무해 응답).
 * <p>alreadyProcessed: 이번 호출 전에 이미 취소/종료되어 변경 행이 없던 경우 true.
 */
@Getter
@Builder
public class TbmActionResponse {
    private final boolean success;
    private final boolean alreadyProcessed;
}
