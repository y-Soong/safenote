package com.prafta.app.req.req07.dto.response.result;

/**
 * prafta-app-030 후속: 대기중(미승인) 초과근무 신청 1건 — 신규 OT 신청 시 표시 전용.
 *
 * <p>출처 = TB_USER_ATTD_REQ(REQ_TYPE IN ('03','04') AND REQ_STATUS='01'). 승인분('02')은
 * TB_USER_OVERTIME_MGMT 로 적재되어 AppliedOvertimeResult 로 별도 노출되므로, 본 결과는
 * 대기('01')만 조회한다 → 이중표시 없음.
 *
 * <p>구간은 신청 시작/종료(START_ ~ END_) 일자·시각으로 표현한다:
 * <ul>
 *   <li>startDate 'YYYYMMDD' / startTime 'HHmm'.</li>
 *   <li>endDate 'YYYYMMDD' / endTime 'HHmm' — 본 조회는 START/END_DATE·TIME IS NOT NULL 행만 반환한다.</li>
 * </ul>
 * 오버나이트(날짜 넘김)는 startDate/endDate 가 일자를 각각 보유하므로 (일자+시각) 결합 비교로 처리한다.
 *
 * <p>★스코프 한정: 대기중 OT 는 표시 전용이다. 신규 슬롯 겹침 사전차단(slotExistingOverlap)에는
 *    포함하지 않는다 — 같은 날 대기중 OT 등록이 있으면 서버 countDuplicateReq 가 신규 제출을
 *    ATTD_400_090 으로 이미 차단하므로 겹침 메시지는 부정확하다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 *    AppReq07Mapper.selectPendingOvertimeReqs 의 SELECT 컬럼 순서를 아래 인자 순서와 100% 일치시킬 것
 *    (START_DATE, START_TIME, END_DATE, END_TIME).
 */
public record PendingOvertimeResult(
        String startDate   // START_DATE YYYYMMDD
        , String startTime // START_TIME HHmm
        , String endDate   // END_DATE YYYYMMDD
        , String endTime   // END_TIME HHmm
) {
}
