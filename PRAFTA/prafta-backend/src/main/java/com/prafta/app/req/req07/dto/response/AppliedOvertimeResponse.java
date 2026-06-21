package com.prafta.app.req.req07.dto.response;

import java.util.List;

import com.prafta.app.req.req07.dto.response.result.AppliedOvertimeResult;
import com.prafta.app.req.req07.dto.response.result.PendingOvertimeResult;

/**
 * prafta-app-030: 초과근무 신청 폼의 "이미 등록된 초과근무" 목록 응답.
 *
 * <p>식별값(cmpnyCd/siteCd/userCd)은 JWT 에서만 도출하므로 응답에 포함하지 않는다(IDOR).
 * 빈 결과는 200 + 빈 배열(예외 아님). 프론트는 이 목록을 읽기전용 표시 + 신규 슬롯 겹침 경고에 사용한다.
 *
 * <p>pendingOvertimes = 대기중(REQ_STATUS='01') OT 신청, 표시 전용(겹침 사전차단 비대상).
 */
public record AppliedOvertimeResponse(
        List<AppliedOvertimeResult> overtimes
        , List<PendingOvertimeResult> pendingOvertimes
) {
}
