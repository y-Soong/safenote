package com.prafta.common.cmm.attd.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BW-05(2026-09-04): 인정시간 "실제로 쉰 휴게만 공제" 확장 산식 검증(요청서 §1-4 / plan §7 Q-3).
 *
 * <p>기준: 웹 Attd_08 {@code recognizedMin} + {@code schedBreakDeduct} 산식과의 파리티 계약.
 * 기존 9-인자 시그니처는 12-인자(휴게 시각 null·반차 체크 false) 위임이라 종전 답과 바이트 동일해야 한다
 * ({@link RecognizedMinutesUtilsTest} 12케이스가 그 무회귀를 고정한다).
 */
class RecognizedMinutesUtilsBreakWaiveTest {

    private static final String YMD = "20260901";

    /** {START, END, USE_UNIT_TYPE, BRK_WAIVE_YN} 창 1건. */
    private static List<String[]> win(String s, String e, String unit, String waive) {
        List<String[]> out = new ArrayList<>();
        out.add(new String[] { s, e, unit, waive });
        return out;
    }

    @Test
    @DisplayName("반차 체크일: 늦게 출근 14~18 근무(스케줄 09~18 휴게 12~13) → 240 (휴게 공제 0)")
    void halfDayWaivedNoBreakDeduction() {
        // 교집합 14:00~18:00 = 240, 연차창 09:00~14:00 은 교집합 밖 → 겹침 0, 휴게 공제 ① 0 → 240
        List<String[]> wins = win("0900", "1400", "01", "Y");
        assertTrue(RecognizedMinutesUtils.halfDayBreakWaived(wins));
        assertEquals(240, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "1400", null, "1800", "0900", "1800", 60, "1200", "1300", wins, true));
    }

    @Test
    @DisplayName("반차 미체크일: 14~18 근무 — 휴게 12~13 이 실근태 밖이라 공제 0 → 240 (종전 180 에서 정정)")
    void halfDayNotWaivedBreakOutsideActual() {
        List<String[]> wins = win("0900", "1345", "01", "N");
        assertFalse(RecognizedMinutesUtils.halfDayBreakWaived(wins));
        assertEquals(240, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "1400", null, "1800", "0900", "1800", 60, "1200", "1300", wins, false));
    }

    @Test
    @DisplayName("종일 근무 09~18, 휴게 시각 12~13 → 540 − 60 = 480 (시각 교집합 = 종전 분 공제와 동일)")
    void fullDayBreakByTime() {
        assertEquals(480, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, "1200", "1300", List.of(), false));
    }

    @Test
    @DisplayName("휴게 분만 등록(시각 없음) 60 → 종전과 동일 480 (③ 폴백)")
    void minutesOnlyFallback() {
        assertEquals(480, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, null, null, List.of(), false));
        // 9-인자 위임 == 12-인자(null, null, false)
        assertEquals(RecognizedMinutesUtils.recognizedMinutes(
                        YMD, null, "0900", null, "1800", "0900", "1800", 60, List.of()),
                RecognizedMinutesUtils.recognizedMinutes(
                        YMD, null, "0900", null, "1800", "0900", "1800", 60, null, null, List.of(), false));
    }

    @Test
    @DisplayName("일찍 퇴근 09~12 근무(휴게 12~13) → 휴게가 실근태 밖 → 180 (종전 120)")
    void earlyLeaveBreakOutsideActual() {
        assertEquals(180, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1200", "0900", "1800", 60, "1200", "1300", List.of(), false));
    }

    @Test
    @DisplayName("휴게 일부만 쉼: 09~12:30 근무(휴게 12~13) → 210 − 30 = 180")
    void partialBreakRested() {
        assertEquals(180, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1230", "0900", "1800", 60, "1200", "1300", List.of(), false));
    }

    @Test
    @DisplayName("시간차 체크(예 A): 저장 13~18 창, 09~13 근무 → 240 (편입 휴게 13~14 는 유효 소정 밖 — 추가 면제 없음)")
    void hourlyWaivedMergedBreakExcludedAutomatically() {
        List<String[]> wins = win("1300", "1800", "02", "Y");
        assertFalse(RecognizedMinutesUtils.halfDayBreakWaived(wins)); // 시간차는 ① 비대상
        assertEquals(240, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1300", "0900", "1800", 60, "1300", "1400", wins, false));
    }

    @Test
    @DisplayName("연차창 안의 휴게 이중 공제 금지: 11~18 근무, 반차 미체크 창 09~13:45, 휴게 12~13 → 420 − 165 − 0 = 255")
    void breakInsideLeaveWindowNotDoubleDeducted() {
        List<String[]> wins = win("0900", "1345", "01", "N");
        assertEquals(255, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "1100", null, "1800", "0900", "1800", 60, "1200", "1300", wins, false));
    }

    @Test
    @DisplayName("야간 22~06 휴게 '2330'~'0030'(G-1 wrap) 전 구간 근무 → 480 − 60 = 420")
    void nightBreakAcrossMidnight() {
        assertEquals(420, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "2200", null, "0600", "2200", "0600", 60, "2330", "0030", List.of(), false));
    }

    @Test
    @DisplayName("야간 22~06 휴게 '0100'~'0200'(자정 이후 시각 — 구간 프레임 이동) 전 구간 근무 → 420")
    void nightBreakAfterMidnightShiftedIntoFrame() {
        assertEquals(420, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "2200", null, "0600", "2200", "0600", 60, "0100", "0200", List.of(), false));
    }

    @Test
    @DisplayName("휴게 종료 '2400' 파싱: 20~24 스케줄 휴게 23:00~24:00, 전 구간 근무 → 240 − 60 = 180")
    void breakEnd2400Parsed() {
        assertEquals(180, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "2000", null, "2400", "2000", "2400", 60, "2300", "2400", List.of(), false));
    }

    @Test
    @DisplayName("0폭 휴게 시각('0000'~'0000')은 시각 없음 취급 → 분 공제 폴백 480")
    void zeroWidthBreakFallsBackToMinutes() {
        assertEquals(480, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, "0000", "0000", List.of(), false));
    }

    @Test
    @DisplayName("halfDayBreakWaived: 구 2-원소 창/시간차 Y/반차 N 은 false, 반차 Y 만 true")
    void halfDayBreakWaivedPredicate() {
        List<String[]> legacy = new ArrayList<>();
        legacy.add(new String[] { "0900", "1300" });
        assertFalse(RecognizedMinutesUtils.halfDayBreakWaived(legacy));
        assertFalse(RecognizedMinutesUtils.halfDayBreakWaived(win("1400", "1800", "02", "Y")));
        assertFalse(RecognizedMinutesUtils.halfDayBreakWaived(win("0900", "1345", "01", "N")));
        assertTrue(RecognizedMinutesUtils.halfDayBreakWaived(win("0900", "1400", "01", "Y")));
        assertFalse(RecognizedMinutesUtils.halfDayBreakWaived(null));
    }
}
