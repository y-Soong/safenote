package com.prafta.app.admin.employeestatus.result;

/**
 * PRAFTA-002: 직원 현황(일자) 로스터 원시 행 — 스코프 내 활성 사용자 전체가 시작점(TB_USER).
 *
 * <p>plan §0-3-3 근거: 기존 {@code AppAdminAttdServiceImpl.selectDaily}는 {@code TB_USER_ATTD_MGMT}(근태
 * 실적 행)가 시작점이라 그날 출근 기록이 아예 없는 사람(미출근/휴무)이 응답에서 빠진다. 본 화면은 4개 상태
 * (근무중/미출근/휴무/퇴근)를 전부 보여줘야 하므로 활성 사용자 전체를 LEFT JOIN 으로 붙인다.
 *
 * <p>상태(근무중/미출근/휴무/퇴근) 산출은 SQL CASE 가 아니라 서비스(Java)에서 한다(기존 관례 — SQL 은
 * 원자재만 내려준다).
 *
 * <p>⚠️ record 는 위치 매핑이다 — SELECT 컬럼 순서와 필드 순서를 반드시 일치시킬 것.
 */
public record EmployeeStatusRosterRow(
      String userCd
    , String userNm
    , String nodeNm
    /** 오늘 근무계획(TB_USER_WORK_PLAN) 존재 여부 — 'Y'/'N'. 'N'이면 휴무(DAY_OFF). */
    , String hasWorkPlanYn
    /** 출근 HHMM(1차 대표행, 2구간은 1차 범위 밖 — plan §PRAFTA-002 "1차 범위 결정"). null=미출근. */
    , String checkInTime
    /** 퇴근 HHMM(1차 대표행). null=미퇴근. */
    , String checkOutTime
    /** 외근(그날 GPS 행 존재) 여부 — 'Y'/'N'. */
    , String isOffsiteYn
    /** 연차 계열(종일+반차+시간차) 사용 여부 — 'Y'/'N'(§0-3-2, AppAdminAttdMapper 서브쿼리 그대로 재사용). */
    , String isOnLeaveYn
    /** 그날 그 사용자의 TB_USER_ATTD_MGMT.ATTD_ID 전부(콤마 구분, 2구간이면 최대 2개) — GPS 조회(PRAFTA-003)용. */
    , String attdIds
) {
}
