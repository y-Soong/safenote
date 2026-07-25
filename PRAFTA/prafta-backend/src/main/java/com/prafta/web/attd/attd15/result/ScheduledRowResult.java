package com.prafta.web.attd.attd15.result;

/**
 * ATTD15-T1 - "등록된 스케줄 기준" 원시(raw) 근무일 행.
 *
 * <p>대상 사용자·해당 주(월~일)의 근무일(WORK_PLAN_CD 가 스케줄코드(SCH_CD)인 날)마다 1행.
 * effective-dating 스케줄(현재본+이력 UNION, APPLY_DATE&lt;=근무일 최신 버전) 조회는
 * {@code Attd11Mapper.selectAttdSummaryRows} 의 SCH 서브쿼리를 그대로 이식했다.
 *
 * <p>service 에서 {@code AppAttd01ServiceImpl.plannedMinutes} 와 동일한 계산식(1구간+2구간
 * 근로분, 자정 넘김 보정)을 포팅해 사용자별로 주간 합산한다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 기준({@code feedback_mybatis_record_column_order}).
 */
public record ScheduledRowResult(
        String userCd
        , String workYmd           // YYYYMMDD

        /* 1구간 계획시각/휴게 */
        , String fstSchStrTime     // HHmm
        , String fstSchEndTime     // HHmm
        , String fstSchBrkMin      // 분(문자열)

        /* 2구간 계획시각/휴게 (없으면 전부 null) */
        , String secSchStrTime
        , String secSchEndTime
        , String secSchBrkMin
) {
}
