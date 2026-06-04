package com.prafta.app.req.req07.dto.response;

import java.util.List;

import com.prafta.app.req.req07.dto.response.result.SchedOptionResult;

/**
 * prafta-app-007 F2: 스케줄 수정 요청 폼의 "스케줄 선택" 목록 응답.
 *
 * <p>식별값(cmpnyCd/siteCd)은 JWT 에서만 도출하므로 응답에 포함하지 않는다.
 * 빈 결과는 200 + 빈 배열 (예외 아님).
 */
public record SchedOptionResponse(
        List<SchedOptionResult> schedules
) {
}
