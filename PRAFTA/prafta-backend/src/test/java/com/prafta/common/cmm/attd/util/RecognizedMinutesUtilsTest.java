package com.prafta.common.cmm.attd.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인정시간(분) 산식 검증 (PRAFTA-SUBCON-T8-1 plan T3 — 12케이스).
 *
 * <p>기준: 웹 Attd_08 {@code recognizedMin} 산식(2026-08-17 시간차 면제 확장 반영분)과의 파리티 계약.
 * 각 케이스 기대값은 Attd_08 산식에 손으로 대입한 값을 주석으로 병기한다.
 */
class RecognizedMinutesUtilsTest {

    private static final String YMD = "20260801";
    private static final String NEXT_YMD = "20260802";

    /** 연차창 1건 헬퍼. */
    private static List<String[]> wins(String... startEndPairs) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < startEndPairs.length; i += 2) {
            out.add(new String[] { startEndPairs[i], startEndPairs[i + 1] });
        }
        return out;
    }

    @Test
    @DisplayName("케이스1 정상 주간: 0900~1800 실근태, 스케줄 0900~1800, 휴게 60, 연차 없음 → 480")
    void case1Normal() {
        // 교집합 = 540~1080 = 540분, 540 − 60 − 0 = 480
        assertEquals(480, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, List.of()));
    }

    @Test
    @DisplayName("케이스2 야간 스케줄(2200~0600 wrap ②) + 퇴근 익일 일자 기재 → 420")
    void case2NightWithOutDate() {
        // inM=1320, outM=1440+360=1800(일자 기재), schStart=1320, schEnd=360→wrap→1800
        // 교집합 = 1320~1800 = 480분, 480 − 60 = 420
        assertEquals(420, RecognizedMinutesUtils.recognizedMinutes(
                YMD, YMD, "2200", NEXT_YMD, "0600", "2200", "0600", 60, List.of()));
    }

    @Test
    @DisplayName("케이스3 야간 + 퇴근 일자 미기재(퇴근<출근 wrap ① 폴백) → 케이스2 와 동일 420")
    void case3NightWithoutOutDate() {
        // outM=360<inM=1320 → wrap ① → 1800. 이후 케이스2 와 동일.
        assertEquals(420, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "2200", null, "0600", "2200", "0600", 60, List.of()));
    }

    @Test
    @DisplayName("케이스4 오전 반차(0900~1300 창) 겹침 차감 → 540 − 60 − 240 = 240")
    void case4MorningHalfLeave() {
        // 교집합 540~1080(=540분), 창 540~780 겹침 240분
        assertEquals(240, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, wins("0900", "1300")));
    }

    @Test
    @DisplayName("케이스5 시간차 2h(1400~1600 창) 겹침 차감 → 540 − 60 − 120 = 360")
    void case5HourlyLeave() {
        // 창 840~960 겹침 120분
        assertEquals(360, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, wins("1400", "1600")));
    }

    @Test
    @DisplayName("케이스6 조퇴로 연차창이 (실제∩스케줄) 밖(1500 퇴근, 창 1600~1800) → 차감 0, 과차감 없음")
    void case6LeaveOutsideIntersection() {
        // 교집합 540~900(=360분), 창 960~1080 은 교집합 밖 → 차감 0. 360 − 60 = 300
        assertEquals(300, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1500", "0900", "1800", 60, wins("1600", "1800")));
    }

    @Test
    @DisplayName("케이스7 미퇴근(checkOutTime null 및 '') → null (0 아님)")
    void case7MissingCheckOut() {
        assertNull(RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, null, "0900", "1800", 60, List.of()));
        assertNull(RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "", "0900", "1800", 60, List.of()));
    }

    @Test
    @DisplayName("케이스8 스케줄 시각 빈 문자열('' — 개발 DB 실측 존재) → null")
    void case8BlankSchedule() {
        assertNull(RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "", "1800", 60, List.of()));
        assertNull(RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "", 60, List.of()));
    }

    @Test
    @DisplayName("케이스9 2차수 분리: seq1(0600~1100)·seq2(1200~1700) 각 행이 자기 스케줄 구간만 계상")
    void case9TwoShiftsIndependent() {
        // seq1 행: 실근태 0600~1700(하루 통근무여도) ∩ 스케줄 0600~1100 = 300분, 휴게 0 → 300
        assertEquals(300, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0600", null, "1700", "0600", "1100", 0, List.of()));
        // seq2 행: 실근태 1200~1700 ∩ 스케줄 1200~1700 = 300분 → 300 (행별 독립 산출)
        assertEquals(300, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "1200", null, "1700", "1200", "1700", 0, List.of()));
    }

    @Test
    @DisplayName("케이스10 연차창 종료<=시작(자정 wrap ③ — 등호 포함) → +1440 적용 산출")
    void case10LeaveWindowWrap() {
        // 야간 2200~0600(휴게 0), 창 2300~0100: ws=1380, we=60→wrap→1500.
        // 교집합 1320~1800, 겹침 = min(1800,1500) − max(1320,1380) = 120. 480 − 0 − 120 = 360
        assertEquals(360, RecognizedMinutesUtils.recognizedMinutes(
                YMD, YMD, "2200", NEXT_YMD, "0600", "2200", "0600", 0, wins("2300", "0100")));
        // 등호 케이스: 창 0200~0200(we==ws) → we=120+1440=1560. 겹침 = min(1800,1560) − max(1320,120) = 240.
        // 480 − 0 − 240 = 240
        assertEquals(240, RecognizedMinutesUtils.recognizedMinutes(
                YMD, YMD, "2200", NEXT_YMD, "0600", "2200", "0600", 0, wins("0200", "0200")));
    }

    @Test
    @DisplayName("케이스11 휴게+연차 차감이 겹침 초과 → 0 클램프(음수 금지)")
    void case11ClampToZero() {
        // 교집합 540~600(=60분), 창 540~780 겹침 60분(교집합 한정), 60 − 60 − 60 = −60 → 0
        assertEquals(0, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1000", "0900", "1800", 60, wins("0900", "1300")));
    }

    @Test
    @DisplayName("케이스12 경계 계약: workYmd null → null / leaveWindows null → 차감 0")
    void case12BoundaryContract() {
        assertNull(RecognizedMinutesUtils.recognizedMinutes(
                null, null, "0900", null, "1800", "0900", "1800", 60, List.of()));
        // leaveWindows null 은 차감 0 — 케이스1 과 동일 답(null 아님)
        assertEquals(480, RecognizedMinutesUtils.recognizedMinutes(
                YMD, null, "0900", null, "1800", "0900", "1800", 60, null));
    }
}
