package com.prafta.common.cmm.sch.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * BW-10(2026-09-04, G-6): 근무타입 휴게 시각 저장 검증 — 종전 F-2 3규칙 무회귀 + 폭≠분 거부 + '2400'/자정 넘김 파싱.
 */
class BreakTimeValidatorTest {

    private static ApiException reject(Runnable r) {
        ApiException ex = assertThrows(ApiException.class, r::run);
        assertEquals(AttdErrorCode.ATTD_400_197, ex.getErrorCode());
        return ex;
    }

    @Test
    @DisplayName("정상: 09~18 휴게 60 시각 12:00~13:00 → 통과")
    void normalPass() {
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", "1200", "1300"));
    }

    @Test
    @DisplayName("G-6: 휴게 60 인데 시각 12:00~14:00(폭 120) → 400_197 detail 문구")
    void widthMismatchRejected() {
        ApiException ex = reject(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", "1200", "1400"));
        assertTrue(ex.getMessage().contains("구간1 휴게 시각 폭(120분)이 휴게시간(60분)과 다릅니다"), ex.getMessage());
    }

    @Test
    @DisplayName("G-6: 종료 미전송이면 폭 검사 생략(호출부가 파생) — 통과 + deriveBreakEnd = 시작+분")
    void endMissingDerived() {
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", "1200", null));
        assertEquals("1300", BreakTimeValidator.deriveBreakEnd("1200", "60"));
        assertEquals("0030", BreakTimeValidator.deriveBreakEnd("2330", "60")); // 자정 넘김 wrap(보정 SQL 규약)
        assertEquals("1200", BreakTimeValidator.deriveBreakEnd("1200", null)); // 휴게분 없음 = 0폭
        assertNull(BreakTimeValidator.deriveBreakEnd(null, "60"));
    }

    @Test
    @DisplayName("G-1 파싱: 야간 22~06 휴게 60 시각 23:30~00:30(종료<시작 wrap) → 통과 / 23:30~00:00 → 폭 30 거부")
    void nightWrapParsed() {
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", "2200", "0600", "60", "2330", "0030"));
        reject(() -> BreakTimeValidator.validateSegment("구간1", "2200", "0600", "60", "2330", "0000"));
    }

    @Test
    @DisplayName("'2400' 종료: 20~24 근무 휴게 60 시각 23:00~24:00 → 통과(1440 인정)")
    void end2400Accepted() {
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", "2000", "2400", "60", "2300", "2400"));
    }

    @Test
    @DisplayName("무회귀(F-2): 휴게분 > 근무시간 / 시작이 범위 밖 / 시작+분 > 종료 → 각각 거부")
    void legacyRulesKept() {
        assertTrue(reject(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1000", "90", "0900", null))
                .getMessage().contains("근무시간(60분)보다 많을 수 없습니다"));
        assertTrue(reject(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", "1900", null))
                .getMessage().contains("근무시간 범위 안이어야"));
        assertTrue(reject(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", "1730", null))
                .getMessage().contains("근무 종료 시각을 초과"));
    }

    @Test
    @DisplayName("휴게 시각 없음 / 근무시간 비정상은 검증 생략(통과) — NULL 허용 정책")
    void skipWhenNoBreakStartOrBadWork() {
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", null, null));
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", null, "1800", "60", "1200", "1400"));
        assertDoesNotThrow(() -> BreakTimeValidator.validateSegment("구간1", "0900", "0900", "60", "1200", "1400"));
    }

    @Test
    @DisplayName("2구간(schType 02)은 구간2도 검증, 01 은 구간2 무시")
    void twoSegmentRouting() {
        reject(() -> BreakTimeValidator.validate("02", "0400", "0800", "0", null, null,
                "1600", "2000", "30", "1700", "1800")); // 구간2 폭 60 ≠ 30
        assertDoesNotThrow(() -> BreakTimeValidator.validate("01", "0400", "0800", "0", null, null,
                "1600", "2000", "30", "1700", "1800"));
    }

    @Test
    @DisplayName("종료 형식 오류('25:99')는 거부")
    void badEndFormatRejected() {
        reject(() -> BreakTimeValidator.validateSegment("구간1", "0900", "1800", "60", "1200", "2599"));
    }
}
