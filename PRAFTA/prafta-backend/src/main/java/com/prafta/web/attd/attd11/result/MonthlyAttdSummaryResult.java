package com.prafta.web.attd.attd11.result;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 결과 행 (사용자 1명 = 1행).
 *
 * 시간계열은 모두 분(minutes) 단위. "N시간 M분" 표기는 프론트(fmtMinutes)가 한다.
 * 컬럼 정의는 decisions §2 / §9 응답 스펙과 일치.
 */
public record MonthlyAttdSummaryResult(
        /* ── 사용자 정보 ───────────── */
        String userCd
        , String userId
        , String userNm
        , String deptNm
        , String authCd
        , String authNm

        /* ── 월간 근태 종합 지표 ───── */
        , int workDayCnt           // 출근 기록 존재 distinct WORK_YMD 수
        , long workMinutes         // 정규 근무시간(분, 휴게 공제, 초과근무 제외)
        , long otMinutes           // COMPLETED 초과근무 분 합
        , int lateCnt              // 지각 판정 건수(차수 단위)
        , long lateMinutes         // 지각 분 합
        , int earlyLeaveCnt        // 조퇴 판정 건수(차수 단위)
        , long earlyLeaveMinutes   // 조퇴 분 합
        , int absentDayCnt         // 결근이 발생한 일수(결근분 > 0 인 날) — COM-016-F 8-3
        /**
         * HB-06: 결근분 합(분). {@code Σ max(0, 그날 소정근로분 D - 그날 확정 연차 면제분)}.
         * 출근기록이 있는 날은 0(지각·조퇴 지표와 중복 계상 방지).
         */
        , long absentMinutes
        /**
         * HB-06: 결근 일수(소수 1자리). {@code Σ (그날 결근분 / 그날 D)}.
         * 화면 표기는 "0.5일 (3시간 45분)" 처럼 일수 + 시간 병기(B안 확정 2026-08-07).
         */
        , double absentDays
) {
}
