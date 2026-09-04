package com.prafta.common.cmm.leave.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.service.impl.LeaveDeductionServiceImpl;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils.BreakMergeResult;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 시간차 "휴게 건너뛰고 근로 확보" 규칙 + 가로지름 판정 wrap 검증 (부분휴가 휴게 무시 BW-03,
 * 2026-09-04 운영 피드백 A안 개정).
 *
 * <p>기준: 요청서 §1-2·§5-6(야간 휴게 가로지름), plan BW-03, 6차 A안(신청 시작부터 근로를 신청 길이만큼
 * 확보 + 양 끝 맞닿은 휴게 흡수). 분 값은 근무일 00:00 = 0 기준, 익일 +1440.
 *
 * <p>★ 개정 전(접함/겹침 병합 + 차감 = 신청 − 겹침) 기대값이 바뀐 테스트는 {@code crossingBreak…} /
 * {@code requestInsideBreak…} 2건이다 — 휴게가 신청 구간 안에 있을 때 차감이 줄던 결함(2시간차 → 60분)을 고쳤다.
 */
class ScheduleWorkMinutesUtilsMergeBreaksTest {

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

    @Test
    @DisplayName("§5-7 예 A: 09~18 휴게 13~14, 시간차 14~18 체크 → 저장 13:00~18:00, 차감 240, 편입 60, 겹침 0")
    void adjacentBeforeRequestIsMerged() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1300", "1400"), t(14, 0), t(18, 0));
        assertNotNull(r);
        assertEquals(t(13, 0), r.exemptStartMin());
        assertEquals(t(18, 0), r.exemptEndMin());
        assertEquals(240, r.requestMinutes());
        assertEquals(240, r.chargeMinutes());
        assertEquals(0, r.overlapBreakMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
        assertTrue(r.breakTimeRegistered());
        // 저장 HHMM(호출부 규약) 확인.
        assertEquals("1300", ScheduleWorkMinutesUtils.hhmmOfDay(r.exemptStartMin()));
        assertEquals("1800", ScheduleWorkMinutesUtils.hhmmOfDay(r.exemptEndMin()));
    }

    @Test
    @DisplayName("A안: 09~18 휴게 12~13, 시간차 12~14 체크(휴게에서 시작) → 12:00~15:00, 차감 120(불변), 편입 60, 겹침 60")
    void crossingBreakExtendsRangeAndKeepsCharge() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1200", "1300"), t(12, 0), t(14, 0));
        assertNotNull(r);
        assertEquals(t(12, 0), r.exemptStartMin());
        assertEquals(t(15, 0), r.exemptEndMin());
        assertEquals(120, r.requestMinutes()); // 단위 배수 검증은 신청 길이 기준(Q-10)
        assertEquals(120, r.chargeMinutes(), "차감은 항상 신청 길이(클램프 제외)");
        assertEquals(60, r.overlapBreakMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
        assertFalse(r.clamped());
    }

    @Test
    @DisplayName("★운영 피드백 예1: 09~18 휴게 12~13, 11:30 시작 2시간차 체크 → 11:30~14:30, 차감 120, 편입 60")
    void breakInsideRequestExtendsRange() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1200", "1300"), t(11, 30), t(13, 30));
        assertNotNull(r);
        assertEquals(t(11, 30), r.exemptStartMin());
        assertEquals(t(14, 30), r.exemptEndMin());
        assertEquals(120, r.requestMinutes());
        assertEquals(120, r.chargeMinutes(), "종전 규칙은 60분만 차감했다(운영 결함)");
        assertEquals(60, r.overlapBreakMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
        assertFalse(r.clamped());
        assertEquals("1430", ScheduleWorkMinutesUtils.hhmmOfDay(r.exemptEndMin()));
    }

    @Test
    @DisplayName("★운영 피드백 예2: 09~18 휴게 11:30~13:00, 13:00 시작 2시간차 체크 → 앞 휴게 흡수 11:30~15:00, 차감 120, 편입 90")
    void breakTouchingStartIsAbsorbed() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "90", "1130", "1300"), t(13, 0), t(15, 0));
        assertNotNull(r);
        assertEquals(t(11, 30), r.exemptStartMin());
        assertEquals(t(15, 0), r.exemptEndMin());
        assertEquals(120, r.chargeMinutes());
        assertEquals(0, r.overlapBreakMinutes());
        assertEquals(90, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("근무 종료 클램프: 09~18 휴게 17:00~17:30, 16:00 시작 2시간차 → 16:00~18:00, 차감 90(확보 근로분), clamped=true")
    void clampedAtWorkEnd() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "30", "1700", "1730"), t(16, 0), t(18, 0));
        assertNotNull(r);
        assertEquals(t(16, 0), r.exemptStartMin());
        assertEquals(t(18, 0), r.exemptEndMin());
        assertEquals(120, r.requestMinutes());
        assertEquals(90, r.chargeMinutes());
        assertEquals(30, r.waivedBreakMinutes());
        assertTrue(r.clamped());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("2구간 공백 건너뜀: 1구간 09~13(휴게 11~12) + 2구간 14~18, 10:30 시작 2시간차 → 10:30~14:30, 차감 120, 비근로 120")
    void twoSegmentGapIsSkipped() {
        DailyScheduleVO s = sch("0900", "1300", "60", "1100", "1200", "1400", "1800", "0", null, null);
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(s, t(10, 30), t(12, 30));
        assertNotNull(r);
        assertEquals(t(10, 30), r.exemptStartMin());
        assertEquals(t(14, 30), r.exemptEndMin());
        assertEquals(120, r.chargeMinutes());
        assertEquals(120, r.waivedBreakMinutes(), "휴게 60 + 구간 사이 공백 60");
        assertFalse(r.clamped());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("접한 휴게 없음: 09~18 휴게 12~13, 시간차 09~11 → 기록 전용, 차감 120 / 14~18 도 접하지 않아 기록 전용")
    void noAdjacentBreakIsRecordOnly() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1200", "1300"), t(9, 0), t(11, 0));
        assertNotNull(r);
        assertEquals(t(9, 0), r.exemptStartMin());
        assertEquals(t(11, 0), r.exemptEndMin());
        assertEquals(120, r.chargeMinutes());
        assertEquals(0, r.waivedBreakMinutes());
        assertTrue(r.recordOnly());
        assertTrue(r.breakTimeRegistered());

        BreakMergeResult r2 = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1200", "1300"), t(14, 0), t(18, 0));
        assertTrue(r2.recordOnly(), "휴게 12~13 은 14:00 에 접하지 않는다(13:00 ≠ 14:00)");
        assertEquals(240, r2.chargeMinutes());
    }

    @Test
    @DisplayName("신청 뒤에 접한 휴게: 시간차 11~12 + 휴게 12~13 → 11:00~13:00, 차감 60, 편입 60")
    void adjacentAfterRequestIsMerged() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1200", "1300"), t(11, 0), t(12, 0));
        assertEquals(t(11, 0), r.exemptStartMin());
        assertEquals(t(13, 0), r.exemptEndMin());
        assertEquals(60, r.chargeMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("신청 = 휴게 구간(13~14 신청 + 휴게 13~14): 뒤 근로로 확보 → 14:00~15:00, 차감 60, 편입 60")
    void requestInsideBreakShiftsToWork() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1300", "1400"), t(13, 0), t(14, 0));
        assertNotNull(r);
        assertEquals(t(13, 0), r.exemptStartMin());
        assertEquals(t(15, 0), r.exemptEndMin());
        assertEquals(60, r.chargeMinutes(), "종전 규칙은 차감 0(호출부 거부)이었다");
        assertEquals(60, r.overlapBreakMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("차감 0(호출부 ATTD_400_052 거부): 09~18 휴게 17~18, 시간차 17~18 → 이후 근로 없음 → 차감 0")
    void trailingBreakYieldsZeroCharge() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1700", "1800"), t(17, 0), t(18, 0));
        assertNotNull(r);
        assertEquals(0, r.chargeMinutes());
        assertEquals(60, r.overlapBreakMinutes());
        assertTrue(r.clamped());
    }

    @Test
    @DisplayName("반복 병합: 1구간 09~13 휴게 12~13 + 2구간 13~18 휴게 13~14, 시간차 11~12 → 11:00~14:00(두 휴게 연쇄 편입), 차감 60, 편입 120")
    void chainedMergeAcrossTwoBreaks() {
        DailyScheduleVO s = sch("0900", "1300", "60", "1200", "1300", "1300", "1800", "60", "1300", "1400");
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(s, t(11, 0), t(12, 0));
        assertNotNull(r);
        assertEquals(t(11, 0), r.exemptStartMin());
        assertEquals(t(14, 0), r.exemptEndMin());
        assertEquals(60, r.chargeMinutes());
        assertEquals(120, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("§5-6 G-1 야간 22:00~04:30 휴게 '2330'~'0030'(보정 후): 시간차 22:00~23:30 → 22:00~00:30(+1, 저장 '0030'), 차감 90, 편입 60")
    void nightBreakWrapMerged() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("2200", "0430", "60", "2330", "0030"), t(22, 0), t(23, 30));
        assertNotNull(r);
        assertEquals(t(22, 0), r.exemptStartMin());
        assertEquals(DAY + t(0, 30), r.exemptEndMin());
        assertEquals("0030", ScheduleWorkMinutesUtils.hhmmOfDay(r.exemptEndMin()));
        assertEquals(90, r.chargeMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
        assertTrue(r.breakTimeRegistered());
    }

    @Test
    @DisplayName("G-1 보정 전 '2330'~'2400'(폭 30): 편입 30 으로 어긋난다 — prafta-brk-waive-2 보정 SQL 이 필요한 이유")
    void nightBreakStoredAs2400IsRecognizedButShort() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("2200", "0430", "60", "2330", "2400"), t(22, 0), t(23, 30));
        assertEquals(DAY, r.exemptEndMin()); // 00:00+1
        assertEquals(30, r.waivedBreakMinutes());
        assertTrue(r.breakTimeRegistered());
    }

    @Test
    @DisplayName("야간 2구간 프레임: 22:00~04:30(휴게 00~01) / 14:00~19:00(휴게 17~18), 시간차 15:00~17:00(원시 분) → 15:00~18:00 입력 프레임으로 복원")
    void secondSegmentOffsetRestored() {
        DailyScheduleVO s = sch("2200", "0430", "60", "0000", "0100", "1400", "1900", "60", "1700", "1800");
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(s, t(15, 0), t(17, 0));
        assertNotNull(r);
        assertEquals(t(15, 0), r.exemptStartMin());
        assertEquals(t(18, 0), r.exemptEndMin());
        assertEquals(120, r.chargeMinutes());
        assertEquals(60, r.waivedBreakMinutes());
        assertFalse(r.recordOnly());
    }

    @Test
    @DisplayName("G-2 휴게 분만 60(시각 없음): 시간차 14~18 → 기록 전용, 차감 240, breakTimeRegistered=false")
    void minutesOnlyBreakIsRecordOnly() {
        BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", null, null), t(14, 0), t(18, 0));
        assertNotNull(r);
        assertTrue(r.recordOnly());
        assertFalse(r.breakTimeRegistered());
        assertEquals(240, r.chargeMinutes());
        assertEquals(0, r.waivedBreakMinutes());
    }

    @Test
    @DisplayName("산출 불가: 스케줄 null / 종료 ≤ 시작 / 1구간 비정상 → null")
    void notComputable() {
        assertNull(ScheduleWorkMinutesUtils.mergeAdjacentBreaks(null, t(14, 0), t(18, 0)));
        assertNull(ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("0900", "1800", "60", "1200", "1300"), t(14, 0), t(14, 0)));
        assertNull(ScheduleWorkMinutesUtils.mergeAdjacentBreaks(
                sch("xxxx", "1800", "60", "1200", "1300"), t(14, 0), t(18, 0)));
    }

    // ------------------------------------------------------------------
    // crossesBreak(미체크 경로) — overlapsBreak G-1 파서/wrap 반영 확인 (매퍼 mock)
    // ------------------------------------------------------------------

    private static LeaveDeductionServiceImpl serviceWith(DailyScheduleVO sch) {
        LeaveDeductionMapper mapper = mock(LeaveDeductionMapper.class);
        when(mapper.selectDailySchedule(any(), any(), any(), any())).thenReturn(sch);
        return new LeaveDeductionServiceImpl(mapper);
    }

    @Test
    @DisplayName("crossesBreak G-1: 야간 휴게 '2330'~'0030' — 22:00~23:59 가로지름 true, 22:00~23:30 접함 false, '2400' 저장값도 true")
    void crossesBreakNightWrap() {
        LeaveDeductionServiceImpl svc = serviceWith(sch("2200", "0430", "60", "2330", "0030"));
        assertTrue(svc.crossesBreak("C", "S", "U", "20261211", t(22, 0), t(23, 59)));
        assertFalse(svc.crossesBreak("C", "S", "U", "20261211", t(22, 0), t(23, 30)));

        LeaveDeductionServiceImpl svc2400 = serviceWith(sch("2200", "0430", "60", "2330", "2400"));
        assertTrue(svc2400.crossesBreak("C", "S", "U", "20261211", t(22, 0), t(23, 59)),
                "종전에는 '2400' 이 거부되어 가로지름이 놓쳤다");
    }

    @Test
    @DisplayName("crossesBreak 무회귀: 주간 휴게 12~13 — 11~14 true, 09~12 false, 0폭 '0000'~'0000' false, 휴게 시각 없음 false")
    void crossesBreakDaytimeUnchanged() {
        LeaveDeductionServiceImpl svc = serviceWith(sch("0900", "1800", "60", "1200", "1300"));
        assertTrue(svc.crossesBreak("C", "S", "U", "20261211", t(11, 0), t(14, 0)));
        assertFalse(svc.crossesBreak("C", "S", "U", "20261211", t(9, 0), t(12, 0)));
        assertFalse(serviceWith(sch("0300", "1300", "0", "0000", "0000"))
                .crossesBreak("C", "S", "U", "20261211", t(3, 0), t(8, 0)));
        assertFalse(serviceWith(sch("0900", "1800", "60", null, null))
                .crossesBreak("C", "S", "U", "20261211", t(11, 0), t(14, 0)));
        assertFalse(serviceWith(null).crossesBreak("C", "S", "U", "20261211", t(11, 0), t(14, 0)));
    }

    @Test
    @DisplayName("서비스 위임: mergeAdjacentBreaks(6인자) — 스케줄 없으면 null, 있으면 순수 함수 결과")
    void serviceDelegation() {
        assertNull(serviceWith(null).mergeAdjacentBreaks("C", "S", "U", "20261211", t(14, 0), t(18, 0)));
        BreakMergeResult r = serviceWith(sch("0900", "1800", "60", "1300", "1400"))
                .mergeAdjacentBreaks("C", "S", "U", "20261211", t(14, 0), t(18, 0));
        assertNotNull(r);
        assertEquals(t(13, 0), r.exemptStartMin());
        assertEquals(240, r.chargeMinutes());
        assertNull(serviceWith(sch("0900", "1800", "60", "1300", "1400"))
                .mergeAdjacentBreaks("C", "S", "U", "20261211", t(18, 0), t(14, 0)));
    }

    @Test
    @DisplayName("서비스 위임: getHalfDayBoundary(6인자) — 09~18 휴게 12~13 END 체크 13:00 / START 체크 기록 전용 / 스케줄 없음 null")
    void serviceHalfDayBoundaryDelegation() {
        LeaveDeductionServiceImpl svc = serviceWith(sch("0900", "1800", "60", "1200", "1300"));
        ScheduleWorkMinutesUtils.HalfDayBoundary ew = svc.getHalfDayBoundary("C", "S", "U", "20261211",
                ScheduleWorkMinutesUtils.HalfPart.END, true);
        assertEquals(t(13, 0), ew.boundaryMin());
        assertFalse(ew.recordOnly());
        ScheduleWorkMinutesUtils.HalfDayBoundary sw = svc.getHalfDayBoundary("C", "S", "U", "20261211",
                ScheduleWorkMinutesUtils.HalfPart.START, true);
        assertEquals(t(14, 0), sw.boundaryMin());
        assertTrue(sw.recordOnly());
        // 4-인자 기존 메서드와 (END,false) 동일.
        assertEquals(svc.getHalfDayBoundary("C", "S", "U", "20261211"),
                svc.getHalfDayBoundary("C", "S", "U", "20261211", ScheduleWorkMinutesUtils.HalfPart.END, false));
        assertNull(serviceWith(null).getHalfDayBoundary("C", "S", "U", "20261211",
                ScheduleWorkMinutesUtils.HalfPart.END, true));
    }
}
