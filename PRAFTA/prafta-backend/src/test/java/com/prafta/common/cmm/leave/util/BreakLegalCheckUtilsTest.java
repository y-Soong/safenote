package com.prafta.common.cmm.leave.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.util.BreakLegalCheckUtils.Result;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfDayBoundary;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.HalfPart;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * BW-06(2026-09-04): 법정 휴게 하한 경고 산식(요청서 §1-3 / brk_sim.simulate) 검증.
 */
class BreakLegalCheckUtilsTest {

    /** 09~18, 휴게 시각 12~13(60분) — 운영 대표 타입. */
    private static DailyScheduleVO std0918() {
        DailyScheduleVO s = new DailyScheduleVO();
        s.setFstSchStrTime("0900");
        s.setFstSchEndTime("1800");
        s.setFstSchBrkMin("60");
        s.setFstBrkStrTime("1200");
        s.setFstBrkEndTime("1300");
        return s;
    }

    /** 09~18, 휴게 분만 60(시각 없음). */
    private static DailyScheduleVO minutesOnly0918() {
        DailyScheduleVO s = new DailyScheduleVO();
        s.setFstSchStrTime("0900");
        s.setFstSchEndTime("1800");
        s.setFstSchBrkMin("60");
        return s;
    }

    /** 09~18, 휴게 없음(종일 기준 미달 — G-5 제외 대상). */
    private static DailyScheduleVO noBreak0918() {
        DailyScheduleVO s = new DailyScheduleVO();
        s.setFstSchStrTime("0900");
        s.setFstSchEndTime("1800");
        s.setFstSchBrkMin("0");
        return s;
    }

    @Test
    @DisplayName("requiredBreakMinutes: 480↑=60, 240↑=30, 그 외 0")
    void required() {
        assertEquals(60, BreakLegalCheckUtils.requiredBreakMinutes(480));
        assertEquals(60, BreakLegalCheckUtils.requiredBreakMinutes(600));
        assertEquals(30, BreakLegalCheckUtils.requiredBreakMinutes(240));
        assertEquals(30, BreakLegalCheckUtils.requiredBreakMinutes(479));
        assertEquals(0, BreakLegalCheckUtils.requiredBreakMinutes(239));
    }

    @Test
    @DisplayName("§5-1 반차 END 미체크(09~18 휴게 12~13): 경계 14:00(휴게 건너뛰기) → 쉬는 14~18, 잔여 근로 09~12+13~14 = 240, rest 60 ≥ 30 → 경고 없음")
    void endPlainNoWarn() {
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(std0918(), HalfPart.END, false);
        Result r = BreakLegalCheckUtils.evaluate(std0918(), hb.exemptStartMin(), hb.exemptEndMin(), false);
        assertTrue(r.eligible());
        assertEquals(240, r.actualWorkMin());
        assertEquals(60, r.restLeftMin());
        assertEquals(30, r.requiredMin());
        assertTrue(r.ok());
        assertNull(r.message());
        assertEquals("N", r.warnYn());
    }

    @Test
    @DisplayName("§5-1 반차 START 미체크(G-3 끝걷기): 쉬는 09:00~14:00, 잔여 14~18 = 240 무휴게 → 경고(4시간에 30분 없음)")
    void startPlainWarn() {
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(std0918(), HalfPart.START, false);
        Result r = BreakLegalCheckUtils.evaluate(std0918(), hb.exemptStartMin(), hb.exemptEndMin(), false);
        assertTrue(r.eligible());
        assertEquals(240, r.actualWorkMin());
        assertEquals(0, r.restLeftMin());
        assertEquals(30, r.requiredMin());
        assertFalse(r.ok());
        assertEquals("이 날 근로 4시간에 법정 휴게 30분이 없습니다", r.message());
    }

    @Test
    @DisplayName("§5-3 반차 START 체크: 쉬는 09:00~14:00(휴게 포함 걷기 → 경계 14:00 동일 = recordOnly), R=240 체크 → OK(제54조 단서)")
    void startWaiveOkByWaiverRule() {
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(std0918(), HalfPart.START, true);
        Result r = BreakLegalCheckUtils.evaluate(std0918(), hb.exemptStartMin(), hb.exemptEndMin(), true);
        assertTrue(r.eligible());
        assertEquals(240, r.actualWorkMin());
        assertEquals(0, r.restLeftMin());
        assertTrue(r.ok());
    }

    @Test
    @DisplayName("반차 END 체크: 쉬는 13:00~18:00 → 근로 09~13 = 240(휴게 12~13 포함 근로), 체크 && R==240 → OK")
    void endWaiveOk() {
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(std0918(), HalfPart.END, true);
        Result r = BreakLegalCheckUtils.evaluate(std0918(), hb.exemptStartMin(), hb.exemptEndMin(), true);
        assertEquals(240, r.actualWorkMin());
        assertTrue(r.ok());
    }

    @Test
    @DisplayName("시간차 체크 12~14(휴게 12~13 가로지름, 저장 12:00~14:00): 근로 09~12 + 14~18 = 420, rest 0, req 30 → 경고")
    void hourlyWaiveWarn() {
        Result r = BreakLegalCheckUtils.evaluate(std0918(), 12 * 60, 14 * 60, true);
        assertTrue(r.eligible());
        assertEquals(420, r.actualWorkMin());
        assertEquals(30, r.requiredMin());
        assertFalse(r.ok());
        assertEquals("이 날 근로 7시간에 법정 휴게 30분이 없습니다", r.message());
    }

    @Test
    @DisplayName("시간차 미체크 14~16: 근로 조각 09~12 + 13~14 + 16~18 = 360, rest 60 ≥ 30 → OK")
    void hourlyPlainOk() {
        Result r = BreakLegalCheckUtils.evaluate(std0918(), 14 * 60, 16 * 60, false);
        assertEquals(360, r.actualWorkMin());
        assertEquals(60, r.restLeftMin());
        assertTrue(r.ok());
    }

    @Test
    @DisplayName("§5-10 G-5: 휴게 미등록 타입(09~18 휴게 0)은 종일 기준 미달 → 배지 비대상(eligible=false, ok)")
    void g5NoBreakTypeExcluded() {
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(noBreak0918(), HalfPart.START, false);
        Result r = BreakLegalCheckUtils.evaluate(noBreak0918(), hb.exemptStartMin(), hb.exemptEndMin(), false);
        assertFalse(r.eligible());
        assertTrue(r.ok());
        assertNull(r.message());
    }

    @Test
    @DisplayName("분만 타입(휴게 60, 시각 없음) START 미체크: 위치 미상 → rest=60 으로 보아 경고 없음(fail-open)")
    void minutesOnlyFailOpen() {
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(minutesOnly0918(), HalfPart.START, false);
        Result r = BreakLegalCheckUtils.evaluate(minutesOnly0918(), hb.exemptStartMin(), hb.exemptEndMin(), false);
        assertTrue(r.eligible());
        assertEquals(60, r.restLeftMin());
        assertTrue(r.ok());
    }

    @Test
    @DisplayName("evaluateStored: 저장 시각 '1400'~'1800' 미체크 = START 미체크와 동일 경고 / 시각 결손·스케줄 없음은 비대상")
    void evaluateStoredMirrorsEvaluate() {
        Result r = BreakLegalCheckUtils.evaluateStored(std0918(), "0900", "1400", false);
        assertFalse(r.ok());
        assertEquals(240, r.actualWorkMin());
        assertFalse(BreakLegalCheckUtils.evaluateStored(std0918(), null, "1400", false).eligible());
        assertFalse(BreakLegalCheckUtils.evaluateStored(null, "0900", "1400", false).eligible());
    }

    @Test
    @DisplayName("야간 22~06(휴게 '2330'~'0030') END 미체크 저장 '0215'~'0600'(wrap): 프레임 정렬 후 잔여 22:00~02:15 − 휴게 = 195 → req 0 OK")
    void nightWrapAligned() {
        DailyScheduleVO s = new DailyScheduleVO();
        s.setFstSchStrTime("2200");
        s.setFstSchEndTime("0600");
        s.setFstSchBrkMin("60");
        s.setFstBrkStrTime("2330");
        s.setFstBrkEndTime("0030");
        HalfDayBoundary hb = ScheduleWorkMinutesUtils.halfDayBoundary(s, HalfPart.END, false);
        Result viaMin = BreakLegalCheckUtils.evaluate(s, hb.exemptStartMin(), hb.exemptEndMin(), false);
        Result viaStored = BreakLegalCheckUtils.evaluateStored(s,
                ScheduleWorkMinutesUtils.hhmmOfDay(hb.exemptStartMin()),
                ScheduleWorkMinutesUtils.hhmmOfDay(hb.exemptEndMin()), false);
        assertEquals(viaMin.actualWorkMin(), viaStored.actualWorkMin());
        assertEquals(viaMin.ok(), viaStored.ok());
        assertEquals(hb.exemptMinutes(), viaMin.actualWorkMin()); // 잔여 근로 = H(면제분과 대칭)
        assertTrue(viaMin.ok());
    }

    @Test
    @DisplayName("문구: 남은 휴게가 일부 있으면 '부족합니다(남은 휴게 N분)'")
    void messagePartialRest() {
        assertEquals("이 날 근로 8시간 30분에 법정 휴게 60분이 부족합니다(남은 휴게 30분)",
                BreakLegalCheckUtils.buildMessage(510, 60, 30));
    }

    // ================================================================
    // BW-12(§7-1): 단시간(소정 240·휴게 0) 근로자의 휴게 미이용 "상시" 요청 — evaluateDay
    //   정책서 attd/08-leave.md §8.5.10(e)
    // ================================================================

    /** 14:00~18:00, 휴게 0 — 소정 240분 단시간 근무타입(§7-1 대상). */
    private static DailyScheduleVO shortTime1418() {
        DailyScheduleVO s = new DailyScheduleVO();
        s.setFstSchStrTime("1400");
        s.setFstSchEndTime("1800");
        s.setFstSchBrkMin("0");
        return s;
    }

    @Test
    @DisplayName("BW-12: 소정 240·휴게 0 + 상시 요청 없음 → 경고('이 날 근로 4시간에 법정 휴게 30분이 없습니다')")
    void standingDayWarnsWhenNotRequested() {
        Result r = BreakLegalCheckUtils.evaluateDay(shortTime1418(), false, false);
        assertTrue(r.eligible());
        assertFalse(r.ok());
        assertEquals("Y", r.warnYn());
        assertEquals(240, r.actualWorkMin());
        assertEquals(0, r.restLeftMin());
        assertEquals(30, r.requiredMin());
        assertEquals("이 날 근로 4시간에 법정 휴게 30분이 없습니다", r.message());
    }

    @Test
    @DisplayName("BW-12: 소정 240·휴게 0 + 상시 요청 'Y' → 적법(경고 없음, 문구 null)")
    void standingDayOkWhenRequested() {
        Result r = BreakLegalCheckUtils.evaluateDay(shortTime1418(), true, false);
        assertTrue(r.eligible());
        assertTrue(r.ok());
        assertEquals("N", r.warnYn());
        assertNull(r.message());
    }

    @Test
    @DisplayName("BW-12 제외: 240 초과·휴게 0(G-5) / 휴게 등록 타입 / 그날 부분휴가 있음 / 스케줄 없음 → 전부 비대상")
    void standingDayNotEligibleCases() {
        // 09~18 휴게 0 = 소정 540 — 종전 G-5 제외 유지(운영 휴게 미등록 타입 소음 차단)
        assertFalse(BreakLegalCheckUtils.evaluateDay(noBreak0918(), false, false).eligible());
        // 휴게가 등록된 타입은 본 항 대상 아님(부분휴가 축이 담당)
        assertFalse(BreakLegalCheckUtils.evaluateDay(std0918(), false, false).eligible());
        assertFalse(BreakLegalCheckUtils.evaluateDay(minutesOnly0918(), false, false).eligible());
        // 그날 부분휴가가 있으면 건별 배지가 담당 — 일자 축은 비대상
        assertFalse(BreakLegalCheckUtils.evaluateDay(shortTime1418(), false, true).eligible());
        // 스케줄 없음
        assertFalse(BreakLegalCheckUtils.evaluateDay(null, false, false).eligible());
        // 비대상은 항상 경고 없음(ok=true)
        assertTrue(BreakLegalCheckUtils.evaluateDay(noBreak0918(), false, false).ok());
    }
}
