package com.prafta.app.leave.approval.result;

import java.math.BigDecimal;

/**
 * 사용자연차결재-01 (A-2 상세): 연차 본문(연차종류/유급여부/단위/구간) 1행 — selectLeaveBody 포팅.
 *
 * <p>paidYn 은 tb_leave_type_mgmt.PAID_TYPE(SYS023) 을 유급('Y')/무급('N') 으로 정규화한 값이다(F-PAID).
 */
public record LeaveDetailBodyRow(
      String leaveCd
    , String leaveNm
    , String paidYn
    , String useUnitType
    , String unitNm
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    /**
     * 가불(미래 연차 당겨쓰기) 충당 일수 (가불표시-01). 항상 0 이상(null 없음).
     * ★ MyBatis record 위치 기반 매핑 — 반드시 맨 끝 유지(SELECT 맨 끝 컬럼 borrowDays 와 순서 일치).
     */
    , BigDecimal borrowDays
    /**
     * 연차 신청 증빙 필수화(2026-08-29): 증빙 파일 ID(TB_USER_LEAVE_USE.EVIDENCE_FILE_ID). 미첨부면 null.
     * borrowDays 다음(맨 끝) 추가 — 위치매핑 규약 준수.
     */
    , String evidenceFileId
    /** BW-06: 휴게 미이용 요청 'Y'/'N'. ★record 끝(위치매핑). */
    , String brkWaiveYn
    /** BW-06: 휴게 미이용 요청 시각 'yyyy-MM-dd HH:mm'(서버 포맷, 없으면 null). */
    , String brkWaiveReqDtime
    /** v2(BW2-07): 넘긴 휴게 분량(TB_USER_LEAVE_USE.BRK_WAIVE_MIN). v1 행·미요청은 null. ★record 끝(위치매핑). */
    , Integer brkWaiveMin
) {
}
