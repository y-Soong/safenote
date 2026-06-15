package com.prafta.app.attd.admin.result;

/**
 * J1-5: 월별 집계 전 원시(raw) 근태 행 (web attd11 selectAttdSummaryRows 포팅).
 *
 * <p>한 행 = (USER_CD, WORK_YMD, WORK_SEQ) 단위의 출퇴근 실적 + 차수에 맞춰 정규화된 스케줄 계획시각/휴게.
 * service 가 일시 stamp 기준으로 근무일수/근무시간/지각·조퇴를 행별 판정한 뒤 USER_CD 로 집계한다.
 * 실제 출퇴근은 원본 CHECK_IN/OUT. PII 미포함(이름·노드명만).
 */
public record MonthlyAttdRow(
        String userCd
        , String userNm
        , String nodeNm

        , String workYmd          // YYYYMMDD
        , int workSeq             // 1 | 2

        /* 차수에 맞춰 정규화된 스케줄 계획시각/휴게 (HHmm / 분) */
        , String planStart        // HHmm, nullable
        , String planEnd          // HHmm, nullable
        , Integer planBreakMin    // 분, nullable

        /* 실제(원본) 출퇴근 일시 */
        , String actInDate        // YYYYMMDD, nullable
        , String actInTime        // HHmm, nullable
        , String actOutDate       // YYYYMMDD, nullable
        , String actOutTime       // HHmm, nullable
) {
}
