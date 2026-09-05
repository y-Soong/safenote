package com.prafta.common.cmm.leave.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfDayBoundary;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfPart;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 반차 경계 W(분량) 오버로드 검증 (부분휴가 휴게 넘김 v2 BW2-03, 2026-09-05).
 *
 * <p>기준: 요청서 {@code 작업지시서_부분휴가-휴게넘김-v2-법정하한상한제.md} §1-2·R2·R3, plan BW2-03 완료 기준.
 * 기존 {@code ScheduleWorkMinutesUtilsHalfDayWaiveTest}(v1 4조합)는 무수정 통과가 전제이며, 본 파일은
 * boolean 오버로드 ↔ W=B / W=0 위임 동치까지 함께 고정한다. 분 값은 근무일 00:00 = 0 기준.
 */
class ScheduleWorkMinutesUtilsHalfDayWaiveMinTest {

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

    private static HalfDayBoundary hb(DailyScheduleVO s, HalfPart p, int w) {
        HalfDayBoundary r = ScheduleWorkMinutesUtils.halfDayBoundary(s, p, w);
        assertNotNull(r);
        return r;
    }

    /** boolean 오버로드가 int 오버로드(W=B / W=0)와 동치인지(경계·recordOnly·waiveRequested·breakTimeRegistered). */
    private static void assertBooleanDelegation(DailyScheduleVO s, String tag) {
        int b = BreakWaiveCapUtils.companyBreakMinutes(s);
        for (HalfPart p : HalfPart.values()) {
            HalfDayBoundary v1Plain = ScheduleWorkMinutesUtils.halfDayBoundary(s, p, false);
            HalfDayBoundary v2Plain = ScheduleWorkMinutesUtils.halfDayBoundary(s, p, 0);
            HalfDayBoundary v1Waive = ScheduleWorkMinutesUtils.halfDayBoundary(s, p, true);
            HalfDayBoundary v2Waive = ScheduleWorkMinutesUtils.halfDayBoundary(s, p, b);
            assertEquals(v2Plain.boundaryMin(), v1Plain.boundaryMin(), tag + " " + p + " plain");
            assertFalse(v1Plain.waiveRequested(), tag);
            assertFalse(v1Plain.recordOnly(), tag);
            assertEquals(v2Waive.boundaryMin(), v1Waive.boundaryMin(), tag + " " + p + " waive");
            assertTrue(v1Waive.waiveRequested(), tag);
            assertEquals(v2Waive.recordOnly() || b == 0, v1Waive.recordOnly(), tag + " " + p + " recordOnly");
            assertEquals(v2Waive.breakTimeRegistered(), v1Waive.breakTimeRegistered(), tag);
        }
    }

    @Test
    @DisplayName("09~18 휴게 12~13 END: W=0/15/30/45/60 → 14:00/13:45/13:30/13:15/13:00 (movable 60)")
    void nineToSixEndSteps() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        int[][] cases = { { 0, t(14, 0) }, { 15, t(13, 45) }, { 30, t(13, 30) }, { 45, t(13, 15) }, { 60, t(13, 0) } };
        for (int[] c : cases) {
            HalfDayBoundary r = hb(s, HalfPart.END, c[0]);
            assertEquals(c[1], r.boundaryMin(), "END W=" + c[0]);
            assertEquals(60, r.movableBreakMin(), "END W=" + c[0] + " movable");
            assertEquals(c[0], r.waiveMin());
            assertEquals(c[0], r.appliedWaiveMin());
            assertEquals(c[0] > 0, r.waiveRequested());
            assertFalse(r.recordOnly(), "END W=" + c[0] + " recordOnly");
            assertTrue(r.breakTimeRegistered());
            assertEquals(240, r.exemptMinutes());
            // 면제 구간 파생: END = [경계, 종료).
            assertEquals(c[1], r.exemptStartMin());
            assertEquals(t(18, 0), r.exemptEndMin());
        }
    }

    @Test
    @DisplayName("09~18 휴게 12~13 START: W=0/60 → 14:00/14:00 — 휴게가 쉬는 구간 안이라 movable 0·recordOnly(R3)")
    void nineToSixStartRecordOnly() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        HalfDayBoundary r0 = hb(s, HalfPart.START, 0);
        assertEquals(t(14, 0), r0.boundaryMin());
        assertEquals(0, r0.movableBreakMin());
        assertFalse(r0.waiveRequested());
        assertFalse(r0.recordOnly());

        HalfDayBoundary r60 = hb(s, HalfPart.START, 60);
        assertEquals(t(14, 0), r60.boundaryMin());
        assertEquals(0, r60.movableBreakMin());
        assertEquals(60, r60.waiveMin());
        assertEquals(0, r60.appliedWaiveMin());
        assertTrue(r60.waiveRequested());
        assertTrue(r60.recordOnly());
        assertTrue(r60.breakTimeRegistered());
        assertEquals(0, ScheduleWorkMinutesUtils.movableBreakMinutes(s, HalfPart.START));
        assertEquals(60, ScheduleWorkMinutesUtils.movableBreakMinutes(s, HalfPart.END));
    }

    @Test
    @DisplayName("09~18 휴게 12:30~13:30 END: W=30 → 13:30, W=60 → 13:00 (plain 14:00)")
    void breakHalfPastTwelveEnd() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1230", "1330");
        assertEquals(t(14, 0), hb(s, HalfPart.END, 0).boundaryMin());
        assertEquals(t(13, 30), hb(s, HalfPart.END, 30).boundaryMin());
        assertEquals(t(13, 0), hb(s, HalfPart.END, 60).boundaryMin());
    }

    @Test
    @DisplayName("09~18 휴게 14~15 START: plain 13:00, W=30 → 13:30(휴게 머리 14:00~14:30 근로), W=60 → 14:00(v1 체크와 동일)")
    void breakTwoStart() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1400", "1500");
        HalfDayBoundary r0 = hb(s, HalfPart.START, 0);
        assertEquals(t(13, 0), r0.boundaryMin());
        assertEquals(60, r0.movableBreakMin());

        HalfDayBoundary r30 = hb(s, HalfPart.START, 30);
        assertEquals(t(13, 30), r30.boundaryMin());
        assertEquals(30, r30.appliedWaiveMin());
        assertFalse(r30.recordOnly());
        // START 면제 구간 = [시작, 경계).
        assertEquals(t(9, 0), r30.exemptStartMin());
        assertEquals(t(13, 30), r30.exemptEndMin());

        HalfDayBoundary r60 = hb(s, HalfPart.START, 60);
        assertEquals(t(14, 0), r60.boundaryMin());
        assertEquals(t(14, 0), ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.START, true).boundaryMin());

        // END 는 휴게가 쉬는 구간 안 → movable 0, W=30 은 기록 전용.
        HalfDayBoundary e30 = hb(s, HalfPart.END, 30);
        assertEquals(t(13, 0), e30.boundaryMin());
        assertEquals(0, e30.movableBreakMin());
        assertTrue(e30.recordOnly());
    }

    @Test
    @DisplayName("2휴게(09~13 휴게 11~11:30 / 14~18 휴게 16~16:30) END W=60: movable 30(근로 쪽 휴게만)·applied 30·경계 12:30")
    void twoBreaksEnd() {
        DailyScheduleVO s = sch("0900", "1300", "30", "1100", "1130", "1400", "1800", "30", "1600", "1630");
        assertEquals(t(13, 0), hb(s, HalfPart.END, 0).boundaryMin());
        HalfDayBoundary r = hb(s, HalfPart.END, 60);
        assertEquals(30, r.movableBreakMin());
        assertEquals(30, r.appliedWaiveMin());
        assertEquals(60, r.waiveMin());
        assertEquals(t(12, 30), r.boundaryMin());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("G-4 공백 건너뜀(09~13 휴게 12~13 + 14~18) END: plain 14:30, W=30 → 13:00, W=60 → 12:30 (닫힌 식 plain−W 와 다름)")
    void gapSkipG4End() {
        DailyScheduleVO s = sch("0900", "1300", "60", "1200", "1300", "1400", "1800", "0", null, null);
        assertEquals(t(14, 30), hb(s, HalfPart.END, 0).boundaryMin());
        HalfDayBoundary r30 = hb(s, HalfPart.END, 30);
        assertEquals(t(13, 0), r30.boundaryMin());
        assertEquals(60, r30.movableBreakMin());
        HalfDayBoundary r60 = hb(s, HalfPart.END, 60);
        assertEquals(t(12, 30), r60.boundaryMin());
        // 닫힌 식이면 14:30 − 60 = 13:30 이 되어 틀린다.
        assertTrue(r60.boundaryMin() != t(13, 30));
    }

    @Test
    @DisplayName("분만 타입(09~18 brk60, 시각 없음) W=30: plain 13:00 불변·recordOnly·breakTimeRegistered=false·movable 0")
    void minutesOnlyRecordOnly() {
        DailyScheduleVO s = sch("0900", "1800", "60", null, null);
        for (HalfPart p : HalfPart.values()) {
            HalfDayBoundary r = hb(s, p, 30);
            assertEquals(t(13, 0), r.boundaryMin(), p.name());
            assertTrue(r.recordOnly(), p.name());
            assertTrue(r.waiveRequested(), p.name());
            assertFalse(r.breakTimeRegistered(), p.name());
            assertEquals(0, r.movableBreakMin(), p.name());
            assertEquals(0, r.appliedWaiveMin(), p.name());
            assertEquals(30, r.waiveMin(), p.name());
        }
    }

    @Test
    @DisplayName("W > movable 은 movable 로 클램프: 09~18 휴게 12~13 END W=120 → applied 60·경계 13:00, waiveMin 120 echo")
    void clampToMovable() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        HalfDayBoundary r = hb(s, HalfPart.END, 120);
        assertEquals(120, r.waiveMin());
        assertEquals(60, r.appliedWaiveMin());
        assertEquals(60, r.movableBreakMin());
        assertEquals(t(13, 0), r.boundaryMin());
        assertFalse(r.recordOnly());
        // 음수 W 는 0 으로.
        HalfDayBoundary neg = hb(s, HalfPart.END, -15);
        assertEquals(0, neg.waiveMin());
        assertFalse(neg.waiveRequested());
        assertEquals(t(14, 0), neg.boundaryMin());
    }

    @Test
    @DisplayName("boolean 오버로드 위임 동치: waive=false ↔ W=0, waive=true ↔ W=B (대표 타입 6종·양 파트)")
    void booleanDelegation() {
        assertBooleanDelegation(sch("0900", "1800", "60", "1200", "1300"), "09-18 12-13");
        assertBooleanDelegation(sch("0900", "1800", "60", "1130", "1230"), "09-18 1130-1230");
        assertBooleanDelegation(sch("0900", "1800", "60", "1400", "1500"), "09-18 14-15");
        assertBooleanDelegation(sch("0900", "1800", "60", "1300", "1400"), "09-18 13-14");
        assertBooleanDelegation(sch("0900", "1800", "60", null, null), "09-18 분만");
        assertBooleanDelegation(sch("0900", "1300", "30", "1100", "1130", "1400", "1800", "30", "1600", "1630"), "2구간 2휴게");
        assertBooleanDelegation(sch("2200", "0600", "60", "0200", "0300"), "야간 22-06");
        assertBooleanDelegation(sch("2200", "0200", "30", "2330", "0030", "0300", "0600", "0", null, null), "야간 2구간 자정휴게");
    }

    @Test
    @DisplayName("B=0(09~13 휴게 없음) boolean 체크: v1 G-2 의미 보존 — waiveRequested=true·recordOnly=true·경계 불변")
    void noBreakBooleanWaive() {
        DailyScheduleVO s = sch("0900", "1300", "0", null, null);
        HalfDayBoundary r = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, true);
        assertNotNull(r);
        assertEquals(t(11, 0), r.boundaryMin());
        assertTrue(r.waiveRequested());
        assertTrue(r.recordOnly());
        assertFalse(r.breakTimeRegistered());
        assertEquals(0, r.movableBreakMin());
        // int 오버로드 W=0 은 요청 없음.
        HalfDayBoundary r0 = hb(s, HalfPart.END, 0);
        assertFalse(r0.waiveRequested());
        assertFalse(r0.recordOnly());
    }

    @Test
    @DisplayName("HalfDayBoundary 생성자 호환: 4-인자·8-인자 생성자는 분량 3필드 0")
    void recordConstructorCompat() {
        HalfDayBoundary four = new HalfDayBoundary(t(14, 0), t(9, 0), t(18, 0), 240);
        assertEquals(HalfPart.END, four.part());
        assertEquals(0, four.waiveMin());
        assertEquals(0, four.appliedWaiveMin());
        assertEquals(0, four.movableBreakMin());

        HalfDayBoundary eight = new HalfDayBoundary(t(14, 0), t(9, 0), t(18, 0), 240,
                HalfPart.START, true, true, false);
        assertEquals(HalfPart.START, eight.part());
        assertTrue(eight.waiveRequested());
        assertTrue(eight.recordOnly());
        assertEquals(0, eight.waiveMin());
        assertEquals(0, eight.appliedWaiveMin());
        assertEquals(0, eight.movableBreakMin());
    }
}
