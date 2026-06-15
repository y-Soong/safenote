package com.prafta.app.attd.admin.result;

/**
 * J1-5: 일자 근태 현황 집계 전 원시(raw) 근태 행.
 *
 * <p>한 행 = (USER_CD, WORK_YMD, WORK_SEQ) 단위의 출퇴근 실적 + 해당 차수의 유효(effective-dating)
 * 스케줄 계획시각. service 가 일시(YYYYMMDDHHmm) stamp 기준으로 지각/조퇴를 행별 판정한 뒤 사용자(USER_CD)로
 * 집계한다(자정 넘김 야간 오판 방지 — web attd11 규칙 차용, decisions §3-1).
 *
 * <ul>
 *   <li>실제 출퇴근은 원본 CHECK_IN/OUT_DATE+TIME(정산/판정은 raw).</li>
 *   <li>isOutsideYn = 해당 ATTD 의 GPS 행 존재 여부(외근, web attd08 동일 규칙).</li>
 * </ul>
 * <p>PII(휴대폰/이메일)는 SELECT 하지 않는다(이름·노드명만).
 */
public record DailyAttdRow(
        String userCd
        , String userNm
        , String nodeNm

        , String workYmd          // YYYYMMDD
        , int workSeq             // 1 | 2

        /* 차수에 맞춰 정규화된 스케줄 계획시각 (HHmm) */
        , String planStart        // HHmm, nullable
        , String planEnd          // HHmm, nullable

        /* 실제(원본) 출퇴근 일시 — 판정/정산 기준 */
        , String checkInDate      // YYYYMMDD, nullable
        , String checkInTime      // HHmm, nullable
        , String checkOutDate     // YYYYMMDD, nullable
        , String checkOutTime     // HHmm, nullable

        , String isOutsideYn      // 'Y' | 'N'
) {
}
