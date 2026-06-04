package com.prafta.app.req.req07.dto.response.result;

/**
 * prafta-app-019: OT 실근태 범위 검증용 — 해당 (USER_CD, WORK_YMD, WORK_SEQ) 의 실제 근태기록 1건.
 *
 * <p>TB_USER_ATTD_MGMT 의 원본 출퇴근 시각([CHECK_IN ~ CHECK_OUT]). OT 슬롯이 이 범위 안에
 * 포함되는지(차집합 = 실근태 − 스케줄) 검증하는 데 사용한다. 표준화 적용시각이 아닌 원본 시각이다.
 *
 * <p>날짜/시각 필드는 varchar 문자열:
 * <ul>
 *   <li>checkInDate 'YYYYMMDD'(NOT NULL) / checkInTime 'HHmm'(NOT NULL).</li>
 *   <li>checkOutDate 'YYYYMMDD'(nullable) / checkOutTime 'HHmm'(nullable — 미퇴근이면 null).</li>
 * </ul>
 * 퇴근 미완료(checkOutTime null/공백)면 범위 확정 불가 → 그 구간 OT 거부.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 *    AppReq07Mapper.selectActualAttdWindowBySlot 의 SELECT 컬럼 순서를 아래 인자 순서와 100% 일치시킬 것
 *    (CHECK_IN_DATE, CHECK_IN_TIME, CHECK_OUT_DATE, CHECK_OUT_TIME).
 */
public record ActualAttdWindowResult(
        String checkInDate    // YYYYMMDD
        , String checkInTime  // HHmm
        , String checkOutDate // YYYYMMDD (nullable)
        , String checkOutTime // HHmm (nullable)
) {
}
