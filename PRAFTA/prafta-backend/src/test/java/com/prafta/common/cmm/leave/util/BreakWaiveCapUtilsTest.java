package com.prafta.common.cmm.leave.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.util.BreakWaiveCapUtils.CapResult;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;

/**
 * 법정 휴게 하한·넘길 수 있는 상한 cap 산식 검증 (부분휴가 휴게 넘김 v2 BW2-02, 2026-09-05).
 *
 * <p>기준: 요청서 {@code 작업지시서_부분휴가-휴게넘김-v2-법정하한상한제.md} §1-1 산식·R5·R6, §4 시나리오 1·2·3·5,
 * plan BW2-02 완료 기준(R6 경계 239/240/241/479/480/719/720/960 → 0/0/30/30/60/60/90/120).
 */
class BreakWaiveCapUtilsTest {

    private static final int DAY = 1440;

    private static int t(int h, int m) {
        return h * 60 + m;
    }

    private static DailyScheduleVO sch(String fs, String fe, String fb, String fbs, String fbe) {
        return sch(fs, fe, fb, fbs, fbe, null, null, null, null, null);
    }

    private static DailyScheduleVO sch(String fs, String fe, String fb, String fbs, String fbe,
                                       String ss, String se, String sb, String sbs, String sbe) {
        DailyScheduleVO v = new DailyScheduleVO();
        v.setSchCd("T");
        v.setFstSchStrTime(fs);
        v.setFstSchEndTime(fe);
        v.setFstSchBrkMin(fb);
        v.setFstBrkStrTime(fbs);
        v.setFstBrkEndTime(fbe);
        v.setSecSchStrTime(ss);
        v.setSecSchEndTime(se);
        v.setSecSchBrkMin(sb);
        v.setSecBrkStrTime(sbs);
        v.setSecBrkEndTime(sbe);
        return v;
    }

    /** (D, X, F, R, B, req, cap) 7값 일괄 대조. */
    private static void assertCap(CapResult c, String tag, int d, int x, int f, int r, int b, int req, int cap) {
        assertNotNull(c, tag);
        assertEquals(d, c.dailyStdMin(), tag + " D");
        assertEquals(x, c.exemptMin(), tag + " X");
        assertEquals(f, c.fixedOtMin(), tag + " F");
        assertEquals(r, c.remainWorkMin(), tag + " R");
        assertEquals(b, c.companyBreakMin(), tag + " B");
        assertEquals(req, c.requiredMin(), tag + " req");
        assertEquals(cap, c.capMin(), tag + " cap");
    }

    // ------------------------------------------------------------------
    // R6 법정 하한 경계
    // ------------------------------------------------------------------

    @Test
    @DisplayName("R6 requiredBreakMinutes 경계: 239/240/241/479/480/719/720/960 → 0/0/30/30/60/60/90/120")
    void requiredBreakBoundaries() {
        assertEquals(0, BreakWaiveCapUtils.requiredBreakMinutes(239));
        assertEquals(0, BreakWaiveCapUtils.requiredBreakMinutes(240));
        assertEquals(30, BreakWaiveCapUtils.requiredBreakMinutes(241));
        assertEquals(30, BreakWaiveCapUtils.requiredBreakMinutes(479));
        assertEquals(60, BreakWaiveCapUtils.requiredBreakMinutes(480));
        assertEquals(60, BreakWaiveCapUtils.requiredBreakMinutes(719));
        assertEquals(90, BreakWaiveCapUtils.requiredBreakMinutes(720));
        assertEquals(120, BreakWaiveCapUtils.requiredBreakMinutes(960));
        // 음수/0 은 0.
        assertEquals(0, BreakWaiveCapUtils.requiredBreakMinutes(0));
        assertEquals(0, BreakWaiveCapUtils.requiredBreakMinutes(-30));
    }

    @Test
    @DisplayName("R6 8시간 초과 연장: R=600(10h) → floor((600−480)/240)=0 → 60, R=720(12h) → 90 (요청서 §1-1 산식 그대로)")
    void requiredBreakOverEightHours() {
        // ★ 산식이 floor 이므로 8h 초과 ~ 12h 미만은 60 그대로. 12h 도달 시 +30.
        assertEquals(60, BreakWaiveCapUtils.requiredBreakMinutes(600));
        assertEquals(90, BreakWaiveCapUtils.requiredBreakMinutes(720));
        assertEquals(90, BreakWaiveCapUtils.requiredBreakMinutes(959));
    }

    // ------------------------------------------------------------------
    // 요청서 §4 시나리오 (D, F, X, B, req, cap)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("§4-1 09~18 휴게 12~13, 오후 반차(X=240): R=240 → req 0 → cap 60")
    void case1NineToSixHalfDay() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        CapResult c = BreakWaiveCapUtils.compute(s, 0, 240);
        assertCap(c, "case1", 480, 240, 0, 240, 60, 0, 60);
        assertTrue(c.breakTimeRegistered());
    }

    @Test
    @DisplayName("§4-2 같은 타입 + 고정연장 18~20(F=120): R=360 → req 30 → cap 30")
    void case2WithFixedOt() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        // F 는 별도 조회값 — 고정연장 유틸로 환산한 값을 그대로 넘긴다(R5).
        int f = FixedOtScheduleUtils.totalFixedOtMinutes("0900", "1800", null, null, null, null, "1800", "2000");
        assertEquals(120, f);
        CapResult c = BreakWaiveCapUtils.compute(s, f, 240);
        assertCap(c, "case2", 480, 240, 120, 360, 60, 30, 30);
    }

    @Test
    @DisplayName("§4-3 소정 7시간(09~17 휴게 12~13) 반차(X=210): R=210 → req 0 → cap 60(전부)")
    void case3SevenHourDay() {
        DailyScheduleVO s = sch("0900", "1700", "60", "1200", "1300");
        CapResult c = BreakWaiveCapUtils.compute(s, 0, 210);
        assertCap(c, "case3", 420, 210, 0, 210, 60, 0, 60);
    }

    @Test
    @DisplayName("§4-5 시간차: 2시간(X=120) → R=360 req 30 cap 30 / 4시간(X=240) → R=240 req 0 cap 60")
    void case5HourlyLeave() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        assertCap(BreakWaiveCapUtils.compute(s, 0, 120), "case5-2h", 480, 120, 0, 360, 60, 30, 30);
        assertCap(BreakWaiveCapUtils.compute(s, 0, 240), "case5-4h", 480, 240, 0, 240, 60, 0, 60);
    }

    @Test
    @DisplayName("cap 0: 09~18 휴게 12~13 + 고정연장 240(F=240) 반차 → R=480 req 60 cap 0")
    void capZeroByLegal() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        CapResult c = BreakWaiveCapUtils.compute(s, 240, 240);
        assertCap(c, "cap0", 480, 240, 240, 480, 60, 60, 0);
        assertTrue(BreakWaiveCapUtils.guideText(c).contains("법정 휴게 때문에 넘길 수 있는 휴게가 없습니다"));
    }

    // ------------------------------------------------------------------
    // B 정의 / 타입 변형
    // ------------------------------------------------------------------

    @Test
    @DisplayName("분만 타입(09~18 brk60, 시각 없음): B=분 합 60, breakTimeRegistered=false, 안내는 분량 불가")
    void minutesOnlyType() {
        DailyScheduleVO s = sch("0900", "1800", "60", null, null);
        CapResult c = BreakWaiveCapUtils.compute(s, 0, 240);
        assertCap(c, "minOnly", 480, 240, 0, 240, 60, 0, 60);
        assertFalse(c.breakTimeRegistered());
        assertEquals(60, BreakWaiveCapUtils.companyBreakMinutes(s));
        assertTrue(BreakWaiveCapUtils.guideText(c).startsWith("휴게 시각이 등록되지 않아"));
    }

    @Test
    @DisplayName("B=0(09~13 휴게 없음): cap 0, 안내는 '등록된 휴게가 없어'")
    void noBreakAtAll() {
        DailyScheduleVO s = sch("0900", "1300", "0", null, null);
        CapResult c = BreakWaiveCapUtils.compute(s, 0, 120);
        assertCap(c, "B0", 240, 120, 0, 120, 0, 0, 0);
        assertEquals(0, BreakWaiveCapUtils.companyBreakMinutes(s));
        assertTrue(BreakWaiveCapUtils.guideText(c).startsWith("이 날은 등록된 휴게가 없어"));
    }

    @Test
    @DisplayName("휴게 시각 폭이 brkMin 과 다르면 B 는 시각 폭(09~18 brk60, 휴게 시각 12~13:30 → B=90)")
    void companyBreakUsesIntervalWidth() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1330");
        assertEquals(90, BreakWaiveCapUtils.companyBreakMinutes(s));
    }

    @Test
    @DisplayName("2구간 타입(09~13 휴게 11~11:30 / 14~18 휴게 16~16:30): B=60(폭 합), D=420")
    void twoSegmentsTwoBreaks() {
        DailyScheduleVO s = sch("0900", "1300", "30", "1100", "1130", "1400", "1800", "30", "1600", "1630");
        CapResult c = BreakWaiveCapUtils.compute(s, 0, 210);
        assertCap(c, "2seg", 420, 210, 0, 210, 60, 0, 60);
    }

    @Test
    @DisplayName("야간 wrap 타입(22~06 휴게 02~03) + 후방 고정연장 06~08(익일): F=120, R=330 → req 30 → cap 30")
    void nightWrapWithFixedOt() {
        DailyScheduleVO s = sch("2200", "0600", "60", "0200", "0300");
        int f = FixedOtScheduleUtils.totalFixedOtMinutes("2200", "0600", null, null, null, null, "0600", "0800");
        assertEquals(120, f);
        CapResult c = BreakWaiveCapUtils.compute(s, f, 210);
        assertCap(c, "wrap", 420, 210, 120, 330, 60, 30, 30);
        assertTrue(c.breakTimeRegistered());
    }

    @Test
    @DisplayName("compute: 스케줄 null / 1구간 비정상 → null, 음수 F·X 는 0 취급")
    void computeNullAndNegative() {
        assertNull(BreakWaiveCapUtils.compute(null, 0, 240));
        assertNull(BreakWaiveCapUtils.compute(sch(null, "1800", "60", null, null), 0, 240));
        CapResult c = BreakWaiveCapUtils.compute(sch("0900", "1800", "60", "1200", "1300"), -10, -20);
        assertCap(c, "neg", 480, 0, 0, 480, 60, 60, 0);
    }

    // ------------------------------------------------------------------
    // breakMinutesWithin / isValidStep / guideText
    // ------------------------------------------------------------------

    @Test
    @DisplayName("breakMinutesWithin: 09~18 휴게 12~13 — [11:30,14:30)→60, [12:30,17)→30, [13,17)→0, 분만 타입→0")
    void breakMinutesWithinDay() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        assertEquals(60, BreakWaiveCapUtils.breakMinutesWithin(s, t(11, 30), t(14, 30)));
        assertEquals(30, BreakWaiveCapUtils.breakMinutesWithin(s, t(12, 30), t(17, 0)));
        assertEquals(0, BreakWaiveCapUtils.breakMinutesWithin(s, t(13, 0), t(17, 0)));
        assertEquals(0, BreakWaiveCapUtils.breakMinutesWithin(s, t(14, 0), t(13, 0)));
        assertEquals(0, BreakWaiveCapUtils.breakMinutesWithin(sch("0900", "1800", "60", null, null), t(11, 30), t(14, 30)));
        assertEquals(0, BreakWaiveCapUtils.breakMinutesWithin(null, t(11, 30), t(14, 30)));
    }

    @Test
    @DisplayName("breakMinutesWithin: 야간 22~06 휴게 02~03 — 원시 분 [02:00,04:00) 을 +1440 프레임으로 정렬해 60")
    void breakMinutesWithinNightFrame() {
        DailyScheduleVO s = sch("2200", "0600", "60", "0200", "0300");
        // 호출부 원시 분(자정 이후 HHMM → 0 기준) — 근무 구간 [1320, 1800) 과 겹치도록 +1440 정렬.
        assertEquals(60, BreakWaiveCapUtils.breakMinutesWithin(s, t(2, 0), t(4, 0)));
        // 이미 익일 프레임으로 넘어온 값도 동일.
        assertEquals(60, BreakWaiveCapUtils.breakMinutesWithin(s, DAY + t(2, 0), DAY + t(4, 0)));
        // 근무 구간과 전혀 안 겹치는 낮 시간대 → 0.
        assertEquals(0, BreakWaiveCapUtils.breakMinutesWithin(s, t(10, 0), t(12, 0)));
    }

    @Test
    @DisplayName("isValidStep: 0/15/60/720 → true, −15/10/61 → false; STEP_MIN=15")
    void validStep() {
        assertEquals(15, BreakWaiveCapUtils.STEP_MIN);
        assertTrue(BreakWaiveCapUtils.isValidStep(0));
        assertTrue(BreakWaiveCapUtils.isValidStep(15));
        assertTrue(BreakWaiveCapUtils.isValidStep(60));
        assertTrue(BreakWaiveCapUtils.isValidStep(720));
        assertFalse(BreakWaiveCapUtils.isValidStep(-15));
        assertFalse(BreakWaiveCapUtils.isValidStep(10));
        assertFalse(BreakWaiveCapUtils.isValidStep(61));
    }

    @Test
    @DisplayName("guideText: cap>0 은 남는 근로·법정 휴게·회사 휴게·최대치 요약(~됩니다/~없습니다 어미), null → null")
    void guideTextPositive() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        String g = BreakWaiveCapUtils.guideText(BreakWaiveCapUtils.compute(s, 0, 240));
        assertEquals("이 날 남는 근로 4시간 → 법정 휴게 0분. 회사 휴게 60분 중 최대 60분까지 넘길 수 있습니다.", g);
        String g2 = BreakWaiveCapUtils.guideText(BreakWaiveCapUtils.compute(s, 120, 240));
        assertEquals("이 날 남는 근로 6시간 → 법정 휴게 30분. 회사 휴게 60분 중 최대 30분까지 넘길 수 있습니다.", g2);
        String g3 = BreakWaiveCapUtils.guideText(BreakWaiveCapUtils.compute(sch("0900", "1700", "60", "1200", "1300"), 0, 210));
        assertEquals("이 날 남는 근로 3시간 30분 → 법정 휴게 0분. 회사 휴게 60분 중 최대 60분까지 넘길 수 있습니다.", g3);
        assertNull(BreakWaiveCapUtils.guideText(null));
    }
}
