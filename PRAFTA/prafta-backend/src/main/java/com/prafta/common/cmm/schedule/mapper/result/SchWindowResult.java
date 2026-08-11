package com.prafta.common.cmm.schedule.mapper.result;

/**
 * 근무타입(SCH_CD) 의 effective 버전 1건의 1·2구간 시각(HHmm) 윈도우.
 *
 * <p>TB_SCH_MGMT(현재본) + TB_SCH_MGMT_HIST(이력본) 합집합에서 (SCH_CD, asOfYmd)
 * 기준 effective(APPLY_DATE &lt;= asOfYmd 중 최신) 버전의 구간 시각만 추린다.
 * 자정 넘김/2구간 처리는 호출자(ScheduleOverlapGuardServiceImpl)가 분 구간으로 환산한다.
 *
 * <p>SEC_* 가 빈 문자열이면 단일 구간(SCH_TYPE='01')으로 본다(2구간 미존재).
 */
public record SchWindowResult(
        String fstStart
        , String fstEnd
        , String secStart
        , String secEnd

        // PRAFTA-FIXEDOT-2: 고정연장(전방·후방 FROM/TO, HHMM, NULL=없음).
        // 교차일 겹침 판정은 고정연장 포함 "전체 점유" 기준(지시서 지점 5).
        // ⚠️ record 끝 = SELECT 끝 동일 순서(위치 기반 매핑, 중간 삽입 금지).
        , String preFixedOtStart
        , String preFixedOtEnd
        , String fixedOtStart
        , String fixedOtEnd
) {

}
