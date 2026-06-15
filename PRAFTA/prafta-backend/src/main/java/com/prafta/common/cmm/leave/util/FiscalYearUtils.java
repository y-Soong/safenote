package com.prafta.common.cmm.leave.util;

import java.time.LocalDate;

/**
 * 회계연도 윈도우 산출 공통 유틸(모든 잔여계산의 단일 출처).
 *
 * <p>연차 개편(사용자 신청 LEAVE_TYPE='01')에서 "당해 회계연도" 경계를 일관되게 계산하기 위해
 *   {@code LeaveGrantEngineServiceImpl} 의 {@code currentFiscalStart}/{@code safeMonthDay} 동등 로직을
 *   엔진과 독립된 도메인 비종속 유틸로 추출한다(엔진 자체 리팩터는 범위 외 — 본 유틸만 신설).
 *
 * <p>당해 회계연도 = 오늘 기준 {@code [현재 회계연도 시작일, +1년)} (end 는 exclusive).
 *   말일/윤년 클램프 적용(예: 02/29 시작 + 평년 → 02/28). today 1회 주입으로 결정성을 유지한다.
 *
 * <p>모든 메서드는 무상태 정적(static)이다(외부 의존 없음 → 인스턴스/Bean 불필요).
 */
public final class FiscalYearUtils {

    private FiscalYearUtils() {
    }

    /** 시작 월/일 미지정(NULL) 시 폴백 = 1월 1일. */
    public static final int DEFAULT_FISCAL_START_MM = 1;
    public static final int DEFAULT_FISCAL_START_DD = 1;

    /**
     * 회계연도 윈도우(시작 inclusive, 종료 exclusive) 산출 결과.
     *
     * @param fiscalStartYmd       당해 회계연도 시작일(YYYYMMDD, inclusive)
     * @param fiscalEndYmdExclusive 당해 회계연도 종료 경계(YYYYMMDD, exclusive — 시작일 +1년)
     */
    public record FiscalWindow(String fiscalStartYmd, String fiscalEndYmdExclusive) {
    }

    /**
     * 회계연도 윈도우 산출(정수 월/일).
     *
     * @param today   기준일(서버 주입 — 결정성 유지)
     * @param startMm 회계연도 시작 월(1~12, 범위 밖이면 클램프)
     * @param startDd 회계연도 시작 일(1~말일, 범위 밖이면 클램프)
     */
    public static FiscalWindow fiscalWindow(LocalDate today, int startMm, int startDd) {
        LocalDate start = currentFiscalStart(today, startMm, startDd);
        // +1년 후 동일 월/일을 다시 클램프(예: 02/29 시작 → 익년 평년이면 02/28).
        LocalDate endExclusive = safeMonthDay(start.getYear() + 1, startMm, startDd);
        return new FiscalWindow(toYmd(start), toYmd(endExclusive));
    }

    /**
     * 회계연도 윈도우 산출(문자열 월/일 — 활성정책 AXIS2_FISCAL_START_MM/_DD 직접 전달용).
     * 둘 중 하나라도 NULL/공백/숫자아님이면 1월 1일 폴백.
     */
    public static FiscalWindow fiscalWindow(LocalDate today, String startMm, String startDd) {
        int mm = parseOr(startMm, DEFAULT_FISCAL_START_MM);
        int dd = parseOr(startDd, DEFAULT_FISCAL_START_DD);
        return fiscalWindow(today, mm, dd);
    }

    /** 오늘 기준 현재 회계연도 시작일(오늘 포함 이전의 가장 최근 startMm/startDd). */
    public static LocalDate currentFiscalStart(LocalDate today, int startMm, int startDd) {
        LocalDate thisYearStart = safeMonthDay(today.getYear(), startMm, startDd);
        if (!thisYearStart.isAfter(today)) {
            return thisYearStart;
        }
        return safeMonthDay(today.getYear() - 1, startMm, startDd);
    }

    /** 윤년/말일 보정(02/29 등): 해당 연·월의 마지막 일을 넘으면 말일로 클램프. */
    public static LocalDate safeMonthDay(int year, int mm, int dd) {
        int m = Math.min(Math.max(mm, 1), 12);
        LocalDate first = LocalDate.of(year, m, 1);
        int last = first.lengthOfMonth();
        int d = Math.min(Math.max(dd, 1), last);
        return LocalDate.of(year, m, d);
    }

    private static int parseOr(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String toYmd(LocalDate d) {
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
