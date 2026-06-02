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
) {
}
