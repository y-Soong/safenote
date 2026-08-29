package com.prafta.app.approval.admin.result;

import java.math.BigDecimal;

/**
 * 001-P2-B3: 연차 상세 본문(연차종류/단위/구간) 1행.
 */
public record LeaveBodyRow(
      String leaveCd
    , String leaveNo
    , String leaveNm
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
) {
}
