package com.prafta.common.cmm.leave.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfDayBoundary;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfPart;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.util.DateTimeUtils;

/**
 * 반차 경계 파트·휴게 무시 체크 4조합 검증 (부분휴가 휴게 무시 도입 BW-02, 2026-09-04).
 *
 * <p>기준: 요청서 {@code 작업지시서_부분휴가-휴게무시.md} §1-1·§3 G-1/G-2/G-3·§5 시나리오 1~6,
 * 기대값 {@code .claude/refs/휴게무시_시뮬레이션/sim_out.txt}(개발 24종+합성 84종)·{@code sim_prod_out.txt}(운영 31종)
 * 의 "미체크경계/체크경계" 열. 단, 휴게 분만 등록 타입의 START 체크 열은 G-2 적용 전 값이라 무시하고
 * 미체크와 같게(recordOnly) 고정한다.
 *
 * <p>분 값은 근무일 00:00 = 0 기준, 익일은 +1440.
 */
class ScheduleWorkMinutesUtilsHalfDayWaiveTest {

    private static final int DAY = 1440;

    /** HH:MM → 분. */
    private static int t(int h, int m) {
        return h * 60 + m;
    }

    /** 익일 HH:MM → 분(+1440). */
    private static int nd(int h, int m) {
        return DAY + t(h, m);
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

    /** 4조합(END 미/체크, START 미/체크)의 경계 분을 한 번에 대조한다. */
    private static void assert4(DailyScheduleVO s, String tag,
                                int endPlain, int endWaive, int startPlain, int startWaive) {
        HalfDayBoundary ep = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, false);
        HalfDayBoundary ew = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, true);
        HalfDayBoundary sp = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.START, false);
        HalfDayBoundary sw = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.START, true);
        assertNotNull(ep, tag);
        assertEquals(endPlain, ep.boundaryMin(), tag + " END 미체크");
        assertEquals(endWaive, ew.boundaryMin(), tag + " END 체크");
        assertEquals(startPlain, sp.boundaryMin(), tag + " START 미체크");
        assertEquals(startWaive, sw.boundaryMin(), tag + " START 체크");
        // 미체크 요청은 recordOnly 가 항상 false, 체크 요청은 "경계 불변 ↔ recordOnly" 가 1:1.
        assertFalse(ep.recordOnly(), tag);
        assertFalse(sp.recordOnly(), tag);
        assertEquals(endPlain == endWaive, ew.recordOnly(), tag + " END recordOnly");
        assertEquals(startPlain == startWaive, sw.recordOnly(), tag + " START recordOnly");
        assertTrue(ew.waiveRequested() && sw.waiveRequested(), tag);
        assertFalse(ep.waiveRequested() || sp.waiveRequested(), tag);
        // 파트 echo + 면제 구간 파생.
        assertEquals(HalfPart.END, ep.part());
        assertEquals(HalfPart.START, sp.part());
        assertEquals(ep.boundaryMin(), ep.exemptStartMin(), tag);
        assertEquals(ep.workEndMin(), ep.exemptEndMin(), tag);
        assertEquals(sp.workStartMin(), sp.exemptStartMin(), tag);
        assertEquals(sp.boundaryMin(), sp.exemptEndMin(), tag);
    }

    @Test
    @DisplayName("§5-1 09~18 휴게 12~13: END 미/체크 14:00/13:00, START 미/체크 14:00/14:00(기록 전용)")
    void nineToSixBreakNoon() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1200", "1300");
        assert4(s, "09-18 휴게12-13", t(14, 0), t(13, 0), t(14, 0), t(14, 0));
        HalfDayBoundary ew = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, true);
        assertTrue(ew.breakTimeRegistered());
        assertEquals(240, ew.exemptMinutes());
        assertEquals(t(9, 0), ew.workStartMin());
        assertEquals(t(18, 0), ew.workEndMin());
    }

    @Test
    @DisplayName("§5-2 09~18 휴게 11:30~12:30(유급 30분은 근무 등록): END 체크 13:00, START 체크 = 미체크 14:00")
    void nineToSixBreakHalfPastEleven() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1130", "1230");
        assert4(s, "09-18 휴게1130-1230", t(14, 0), t(13, 0), t(14, 0), t(14, 0));
    }

    @Test
    @DisplayName("§5-3 09~18 휴게 14~15: START 체크 14:00(미체크 13:00), END 체크 13:00 그대로(기록 전용)")
    void nineToSixBreakTwo() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1400", "1500");
        assert4(s, "09-18 휴게14-15", t(13, 0), t(13, 0), t(13, 0), t(14, 0));
    }

    @Test
    @DisplayName("§5-4 G-3 09~18 휴게 13~14(경계 접함): START 미체크 14:00(종전 시작걷기 13:00 에서 변경), END 미체크 13:00")
    void nineToSixBreakOneG3() {
        DailyScheduleVO s = sch("0900", "1800", "60", "1300", "1400");
        assert4(s, "09-18 휴게13-14", t(13, 0), t(13, 0), t(14, 0), t(14, 0));
        // 종전 공용 경계(END 미체크)는 13:00 — START 에 공용하면 출근 직후 휴게가 되던 결함이 G-3 로 해소된다.
        assertEquals(t(13, 0), ScheduleWorkMinutesUtils.halfDayBoundary(s).boundaryMin());
    }

    @Test
    @DisplayName("§5-5 G-2 09~18 분만 60(시각 없음): 4조합 전부 13:00, 체크는 기록 전용 + breakTimeRegistered=false")
    void nineToSixMinutesOnly() {
        DailyScheduleVO s = sch("0900", "1800", "60", null, null);
        assert4(s, "09-18 분만60", t(13, 0), t(13, 0), t(13, 0), t(13, 0));
        HalfDayBoundary sw = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.START, true);
        assertTrue(sw.recordOnly());
        assertFalse(sw.breakTimeRegistered());
        // 근무 종료는 조각 끝(17:00)이 아니라 구간 끝(18:00)이어야 한다(HB-01 규약 유지).
        assertEquals(t(18, 0), sw.workEndMin());
    }

    @Test
    @DisplayName("G-2 09~18 휴게 미등록(분 0·시각 없음): 4조합 13:30, 체크 기록 전용")
    void nineToSixNoBreak() {
        DailyScheduleVO s = sch("0900", "1800", null, null, null);
        assert4(s, "09-18 휴게없음", t(13, 30), t(13, 30), t(13, 30), t(13, 30));
        assertFalse(ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, true).breakTimeRegistered());
    }

    @Test
    @DisplayName("§5-6 G-1 야간 22:00~04:30 휴게 23:30~00:30(보정 후 '2330'~'0030'): END 미/체크 01:45+1/00:45+1, START 01:45+1 기록 전용")
    void nightBreakAcrossMidnight() {
        DailyScheduleVO s = sch("2200", "0430", "60", "2330", "0030");
        assert4(s, "야간 휴게2330-0030", nd(1, 45), nd(0, 45), nd(1, 45), nd(1, 45));
        HalfDayBoundary ep = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, false);
        assertTrue(ep.breakTimeRegistered(), "wrap 파서로 야간 휴게가 시각 휴게로 인식되어야 한다");
        assertEquals(165, ep.exemptMinutes());
        assertEquals(t(22, 0), ep.workStartMin());
        assertEquals(nd(4, 30), ep.workEndMin());
    }

    @Test
    @DisplayName("G-1 보정 전 저장값 '2330'~'2400'(폭 30 ≠ 분 60): '2400' 을 1440 으로 인식 — 경계 01:15+1(종전 분만 취급 00:45+1 에서 변경)")
    void nightBreakStoredAs2400() {
        DailyScheduleVO s = sch("2200", "0430", "60", "2330", "2400");
        // parts = [22:00,23:30](90) + [00:00+1,04:30+1](270), H=165 → END 미체크 = 00:00+1 + 75 = 01:15+1.
        //   보정 SQL(prafta-brk-waive-2) 적용 후에는 위 nightBreakAcrossMidnight 값이 된다 — 같은 창 적용 필요.
        assert4(s, "야간 휴게2330-2400", nd(1, 15), nd(0, 45), nd(1, 45), nd(1, 45));
        assertTrue(ScheduleWorkMinutesUtils.halfDayBoundary(s).breakTimeRegistered());
    }

    @Test
    @DisplayName("Q-11 ② 2구간 균등 04~08/16~20 휴게 0: START 미체크 16:00(끝걷기, 종전 08:00), END 미체크 08:00")
    void twoSegmentsEqual() {
        DailyScheduleVO s = sch("0400", "0800", "0", null, null, "1600", "2000", "0", null, null);
        assert4(s, "04-08/16-20", t(8, 0), t(8, 0), t(16, 0), t(16, 0));
        HalfDayBoundary sp = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.START, false);
        // 면제 구간 [04:00, 16:00) — 구간 사이 공백을 포함(END 의 [08:00, 20:00) 과 대칭, Q-11 확정).
        assertEquals(t(4, 0), sp.exemptStartMin());
        assertEquals(t(16, 0), sp.exemptEndMin());
    }

    @Test
    @DisplayName("G-3 홀수 D 09:00~17:31 휴게 12:45~13:45(D=451,H=225): END 미체크 12:45, START 미체크 13:46(=종료−225)")
    void oddDailyMinutesWithBreakTime() {
        DailyScheduleVO s = sch("0900", "1731", "60", "1245", "1345");
        assert4(s, "09-1731 휴게1245-1345", t(12, 45), t(12, 45), t(13, 46), t(13, 46));
        assertEquals(225, ScheduleWorkMinutesUtils.halfDayBoundary(s).exemptMinutes());
    }

    @Test
    @DisplayName("G-3 홀수 D 09:00~17:31 분만 60: END 미체크 12:45, START 미체크 12:46(근로 조각 끝 16:31 − 225), 체크 기록 전용")
    void oddDailyMinutesMinutesOnly() {
        DailyScheduleVO s = sch("0900", "1731", "60", null, null);
        assert4(s, "09-1731 분만60", t(12, 45), t(12, 45), t(12, 46), t(12, 46));
    }

    @Test
    @DisplayName("G-4 야간 2구간 휴게 둘(22:00~04:30 휴게 00~01 / 14:00~19:00 휴게 17~18): START 체크 14:15+1(미체크 03:45+1), END 02:45+1/03:45+1")
    void nightTwoSegmentsTwoBreaks() {
        DailyScheduleVO s = sch("2200", "0430", "60", "0000", "0100", "1400", "1900", "60", "1700", "1800");
        assert4(s, "야간2구간 휴게둘", nd(3, 45), nd(2, 45), nd(3, 45), nd(14, 15));
    }

    @Test
    @DisplayName("운영 대표: 09:30~18 휴게 12:00~13:30(90) END 14:30/13:00 · 08~17 휴게 11:30~13:00(90) END 13:15/11:45 · 09~18:30 휴게 12~13 END 14:15/13:15")
    void prodRepresentatives() {
        assert4(sch("0930", "1800", "90", "1200", "1330"), "0930-18 휴게90",
                t(14, 30), t(13, 0), t(14, 30), t(14, 30));
        assert4(sch("0800", "1700", "90", "1130", "1300"), "08-17 휴게90",
                t(13, 15), t(11, 45), t(13, 15), t(13, 15));
        assert4(sch("0900", "1830", "60", "1200", "1300"), "09-1830 휴게60",
                t(14, 15), t(13, 15), t(14, 15), t(14, 15));
        // 휴게 시작 직후(09~10) 타입: END 체크 13:00(미체크 14:00), START 는 휴게가 쉬는 구간 안 → 기록 전용.
        assert4(sch("0900", "1800", "60", "0900", "1000"), "09-18 시작직후60",
                t(14, 0), t(13, 0), t(14, 0), t(14, 0));
        // 휴게 종료 직전(17~18) 타입: START 체크 14:00(미체크 13:00), END 기록 전용.
        assert4(sch("0900", "1800", "60", "1700", "1800"), "09-18 종료직전60",
                t(13, 0), t(13, 0), t(13, 0), t(14, 0));
    }

    @Test
    @DisplayName("0폭 휴게('0000'~'0000')는 휴게 시각 없음으로 본다 — 03~13 휴게0 END/START 08:00, 체크 기록 전용")
    void zeroWidthBreakTreatedAsNone() {
        DailyScheduleVO s = sch("0300", "1300", "0", "0000", "0000");
        assert4(s, "03-13 휴게0", t(8, 0), t(8, 0), t(8, 0), t(8, 0));
        assertFalse(ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, true).breakTimeRegistered());
    }

    @Test
    @DisplayName("무회귀: 기존 halfDayBoundary(sch) == (sch, END, false) — 전 대표 타입 바이트 동일 + 구 생성자 의미(END·미체크)")
    void legacyOverloadRegression() {
        List<DailyScheduleVO> all = List.of(
                sch("0900", "1800", "60", "1200", "1300"),
                sch("0900", "1800", "60", "1130", "1230"),
                sch("0900", "1800", "60", "1400", "1500"),
                sch("0900", "1800", "60", "1300", "1400"),
                sch("0900", "1800", "60", null, null),
                sch("0900", "1800", null, null, null),
                sch("2200", "0430", "60", "2330", "0030"),
                sch("2200", "0430", "60", "2330", "2400"),
                sch("0400", "0800", "0", null, null, "1600", "2000", "0", null, null),
                sch("0900", "1731", "60", "1245", "1345"),
                sch("0900", "1731", "60", null, null),
                sch("2200", "0430", "60", "0000", "0100", "1400", "1900", "60", "1700", "1800"),
                sch("0930", "1800", "90", "1200", "1330"),
                sch("0300", "1300", "0", "0000", "0000"),
                sch("1500", "2400", "0", null, null, "0000", "1800", "", null, null),
                sch("0000", "2400", "0", "0000", "0000"));
        for (DailyScheduleVO s : all) {
            HalfDayBoundary legacy = ScheduleWorkMinutesUtils.halfDayBoundary(s);
            HalfDayBoundary endPlain = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, false);
            assertNotNull(legacy, s.getFstSchStrTime() + "-" + s.getFstSchEndTime());
            assertEquals(endPlain, legacy, "위임 결과 동일: " + s.getFstSchStrTime() + "-" + s.getFstSchEndTime());
            assertEquals(HalfPart.END, legacy.part());
            assertFalse(legacy.waiveRequested());
            assertFalse(legacy.recordOnly());
            // part 미지정(null)도 END 로 본다.
            assertEquals(legacy, ScheduleWorkMinutesUtils.halfDayBoundary(s, null, false));
        }
        // 구 4-인자 생성자 — 기존 호출부 호환(END·미체크 의미).
        HalfDayBoundary old = new HalfDayBoundary(t(14, 0), t(9, 0), t(18, 0), 240);
        assertEquals(HalfPart.END, old.part());
        assertFalse(old.waiveRequested());
        assertFalse(old.recordOnly());
        assertFalse(old.breakTimeRegistered());
        assertEquals(t(14, 0), old.exemptStartMin());
        assertEquals(t(18, 0), old.exemptEndMin());
    }

    @Test
    @DisplayName("무회귀: 종전 산식 대표값(반차 시간대 도입 시뮬) — 15:00~24:00/00:00~18:00 04:30+1 · 24시간 12:00 · 09~14/18~22 13:30")
    void legacyKnownValues() {
        assertEquals(nd(4, 30), ScheduleWorkMinutesUtils.halfDayBoundary(
                sch("1500", "2400", "0", null, null, "0000", "1800", "", null, null)).boundaryMin());
        assertEquals(t(12, 0), ScheduleWorkMinutesUtils.halfDayBoundary(
                sch("0000", "2400", "0", "0000", "0000")).boundaryMin());
        assertEquals(t(13, 30), ScheduleWorkMinutesUtils.halfDayBoundary(
                sch("0900", "1400", "0", null, null, "1800", "2200", "0", null, null)).boundaryMin());
    }

    @Test
    @DisplayName("산출 불가: 스케줄 null / 1구간 비정상 / D<2 → null (체크 여부 무관)")
    void notComputable() {
        assertNull(ScheduleWorkMinutesUtils.halfDayBoundary(null, HalfPart.START, true));
        assertNull(ScheduleWorkMinutesUtils.halfDayBoundary(sch("09xx", "1800", "60", null, null), HalfPart.END, true));
        assertNull(ScheduleWorkMinutesUtils.halfDayBoundary(sch("0900", "0901", "0", null, null), HalfPart.END, false));
    }

    @Test
    @DisplayName("HalfPart.of: START/END 대소문자·공백 허용, 그 외 null")
    void halfPartOf() {
        assertEquals(HalfPart.START, HalfPart.of(" start "));
        assertEquals(HalfPart.END, HalfPart.of("END"));
        assertNull(HalfPart.of("MID"));
        assertNull(HalfPart.of(null));
        assertNull(HalfPart.of(""));
    }

    @Test
    @DisplayName("G-1 DateTimeUtils.brkEndToMinutes: '2400'=1440, '0030'=30, 범위 밖/형식 오류/null 은 null")
    void brkEndParser() {
        assertEquals(1440, DateTimeUtils.brkEndToMinutes("2400"));
        assertEquals(30, DateTimeUtils.brkEndToMinutes("0030"));
        assertEquals(t(23, 59), DateTimeUtils.brkEndToMinutes("2359"));
        assertNull(DateTimeUtils.brkEndToMinutes("2401"));
        assertNull(DateTimeUtils.brkEndToMinutes("2560"));
        assertNull(DateTimeUtils.brkEndToMinutes("12:00"));
        assertNull(DateTimeUtils.brkEndToMinutes(null));
        // 기존 파서는 여전히 '2400' 을 거부한다(시작/실근태/연차 시각 규약 불변).
        assertNull(DateTimeUtils.hhmmToMinutes("2400"));
    }
}
