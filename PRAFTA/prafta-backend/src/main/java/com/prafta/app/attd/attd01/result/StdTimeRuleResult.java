package com.prafta.app.attd.attd01.result;

/**
 * prafta-app-002: 출퇴근 시간 표준화 룰 결과 (TB_ATTD_STD_TIME_RULE + TB_SYST_VAL_D).
 *
 * <p>매핑 대상: AppAttd01Mapper.selectStdTimeRules (회사 단위).
 * <p>stdTimeRuleType[SYS028]: 01=출근/02=퇴근.
 *   unitMinutes 는 SYS029(stdTimeType) 의 VAL_D_INFO_1(분) 을 조인해 가져온 값.
 *   (SYS029: 01=0(설정안함)/02=5/03=10/04=15/05=30). 0 또는 null 이면 표준화 미적용.
 */
public record StdTimeRuleResult(
    String stdTimeRuleType
    , String stdTimeType
    , Integer unitMinutes
) {
}
