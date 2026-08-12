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

        /**
         * PRAFTA-FIXEDOT-3(J2): 실근무 축의 <b>연장 분류</b> 주간 합계(분) =
         * 승인 초과근무(otMinutes) + 고정연장 자동 계상 실적(실근태 ∩ 고정연장 — FixedOtMinutesUtils).
         * 두 구간은 서로 배타(OT 등록범위 = 실근태 − (소정 ∪ 고정연장))라 단순 합산으로 중복이 없다.
         * ★ 총량({@code actualMinutes})에 가산하지 않는다 — 고정연장 근무분은 raw 실근태에 이미
         * 포함되어 있어 "총량 가산"이 아니라 "연장 축 분류" 표시다(주 12h 축 관측용).
         */
        , long actualOtMinutes
) {
}
