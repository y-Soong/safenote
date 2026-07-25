package com.prafta.web.attd.attd15.result;

/**
 * ATTD15-T1 - 주52시간 관리 응답 결과 행(사용자 1명 = 1행).
 *
 * <p>plan.md ATTD15-T1 §핵심요구사항 4)의 응답 스펙과 일치. 시간계열은 모두 분(minutes) 단위 —
 * "N시간 M분" 표기는 프론트(Attd_15.vue fmtDuration)가 담당한다.
 * status 값은 {@code "NORMAL"|"CAUTION"|"DANGER"|"EXCESS"} 문자열 enum.
 */
public record Weekly52hListResult(
        String siteNm
        , String nodeNm
        , String userCd
        , String userId
        , String userNm
        , String weekStartYmd     // YYYYMMDD (월요일)
        , String weekEndYmd       // YYYYMMDD (일요일)
        , long scheduledMinutes   // 등록된 스케줄 기준 주간 합계(분)
        , String scheduledStatus  // NORMAL | CAUTION | DANGER | EXCESS
        , long actualMinutes      // 실제 근무 기준 주간 합계(분, workMinutes+otMinutes)
        , String actualStatus     // NORMAL | CAUTION | DANGER | EXCESS
        , String provisionalYn    // 마감 전(잠정치) 여부 Y/N — AttdCloseService.isClosedForUser 기준
) {
}
