package com.prafta.app.req.req07.dto.response.result;

/**
 * prafta-app-030: 이미 등록(적용)된 초과근무 1건 — 신규 OT 신청 시 표시/겹침 대조 원천.
 *
 * <p>출처 = TB_USER_OVERTIME_MGMT(적용 OT 실적). 대기 OT 요청(tb_user_attd_req)은 countDuplicateReq 가
 * 이미 차단하므로 본 결과의 원천이 아니다(작업지시서 §2).
 *
 * <p>구간은 실제 시작/종료(ACTUAL_*) 일자·시각으로 표현한다:
 * <ul>
 *   <li>startDate 'YYYYMMDD'(NOT NULL) / startTime 'HHmm'(NOT NULL).</li>
 *   <li>endDate 'YYYYMMDD' / endTime 'HHmm' — 본 조회는 ACTUAL_END_DATE/TIME IS NOT NULL 행만 반환하므로 둘 다 채워진다.</li>
 *   <li>workMinutes: 실 근무 분(WORK_MINUTES, nullable). otStatus: OT 상태(CANCELLED 제외).</li>
 * </ul>
 * 오버나이트(날짜 넘김)는 startDate/endDate 가 일자를 각각 보유하므로 (일자+시각) 결합 비교로 처리한다.
 *
 * <p>★스키마 확인: TB_USER_OVERTIME_MGMT 에 WORK_SEQ 컬럼 없음 → workSeq 미포함. 표시 식별은 시각으로 충분.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 *    AppReq07Mapper.selectAppliedOvertimes 의 SELECT 컬럼 순서를 아래 인자 순서와 100% 일치시킬 것
 *    (ACTUAL_START_DATE, ACTUAL_START_TIME, ACTUAL_END_DATE, ACTUAL_END_TIME, WORK_MINUTES, OT_STATUS).
 */
public record AppliedOvertimeResult(
        String startDate       // ACTUAL_START_DATE YYYYMMDD
        , String startTime     // ACTUAL_START_TIME HHmm
        , String endDate       // ACTUAL_END_DATE YYYYMMDD
        , String endTime       // ACTUAL_END_TIME HHmm
        , Integer workMinutes  // WORK_MINUTES (nullable)
        , String otStatus      // OT_STATUS (CANCELLED 제외)
) {
}
