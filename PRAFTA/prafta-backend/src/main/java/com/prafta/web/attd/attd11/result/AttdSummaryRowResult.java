package com.prafta.web.attd.attd11.result;

/**
 * PRAFTA-034 - Attd_11 집계 전 원시(raw) 근태 행.
 *
 * 한 행 = (USER_CD, WORK_YMD, WORK_SEQ) 단위의 출퇴근 실적 + 해당 차수의
 * 유효(effective-dating) 스케줄 계획시각/휴게. service 가 일시(YYYYMMDDHHmm)
 * 기준으로 지각/조퇴/근무시간을 행별 판정한 뒤 USER_CD 로 집계한다.
 *
 * - workSeq 1/2 에 따라 plan{1,2}* / 가 다르므로 service 에서 구간을 선택한다.
 *   단, 본 결과는 SQL 에서 차수에 맞는 plan(시작/종료/휴게)을 이미 단일 컬럼
 *   (planStart/planEnd/planBreakMin)으로 정규화해 내려준다.
 * - 실제 출퇴근은 원본 CHECK_IN/OUT_DATE+TIME (표준화 아님). decisions §3-1.
 */
public record AttdSummaryRowResult(
        String userCd
        , String userId
        , String userNm
        , String deptNm
        , String authCd
        , String authNm

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
           고정연장 실적(자동 계상) 합산·"연장 미이행" 월 카운트 전용 — 판정/집계식 불변.
           ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지). */
        , String fstSchStrTime    // HHmm, nullable
        , String fstSchEndTime
        , String secSchStrTime
        , String secSchEndTime
        , String preFixedOtStrTime
        , String preFixedOtEndTime
        , String fixedOtStrTime
        , String fixedOtEndTime
        , String fixedOtExemptYn  // 'Y'/'N' — 정책 ③ 존재 검사(종일 기간 포함 + 부분 당일)
) {
}
