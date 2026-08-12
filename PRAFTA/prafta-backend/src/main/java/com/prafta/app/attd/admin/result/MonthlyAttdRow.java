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

        /* PRAFTA-FIXEDOT-3: 스케줄 원시 시각 + 고정연장(전방·후방) + 연차 계열 면제 존재('Y'/'N').
           월 실적 합·"연장 미이행" 카운트 파생 전용 — 판정식 불변.
           ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지). */
        , String fstSchStrTime    // HHmm, nullable
        , String fstSchEndTime
        , String secSchStrTime
        , String secSchEndTime
        , String preFixedOtStrTime
        , String preFixedOtEndTime
        , String fixedOtStrTime
        , String fixedOtEndTime
        , String fixedOtExemptYn  // 정책 ③ 존재 검사(종일 기간 포함 + 부분 당일)
) {
}
