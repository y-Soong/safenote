package com.prafta.common.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 모듈 간 공유되는 날짜/시간 헬퍼 클래스.
 *
 * <p>모든 값은 프로젝트 전역의 문자열 컨벤션을 따른다:
 * <ul>
 *   <li>날짜는 yyyyMMdd ({@code String}, 길이 8)</li>
 *   <li>시간은 HHmm ({@code String}, 길이 4, 0 패딩)</li>
 * </ul>
 *
 * <p>"분 stamp(minute stamp)"는 기준 workYmd의 자정(00:00)으로부터 경과한 분(minute)을 의미한다.
 * 같은 날 값은 0..1439, 다음 날 값은 1440..2879에 매핑된다. 이 표현 방식 덕분에
 * LocalDateTime 객체를 다루지 않고도 시간 구간을 비교/병합할 수 있다.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * HHmm 문자열을 자정 기준 분(0..1439)으로 변환한다.
     * 입력이 null이거나 길이가 잘못되었거나, 숫자가 아니거나 범위를 벗어난 경우 null을 반환한다.
     */
    public static Integer hhmmToMinutes(String hhmm) {
        if (hhmm == null || hhmm.length() != 4) return null;
        try {
            int hh = Integer.parseInt(hhmm.substring(0, 2));
            int mm = Integer.parseInt(hhmm.substring(2, 4));
            if (hh < 0 || hh > 23) return null;
            if (mm < 0 || mm > 59) return null;
            return Integer.valueOf(hh * 60 + mm);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * (date - base)의 부호 있는 일수 차이를 반환한다. 두 입력 모두 길이 8의 yyyyMMdd
     * 문자열이어야 한다. 논리적으로 유효한 입력에는 예외를 던지지 않으며, 실제 일수
     * 차이를 반환한다 ({@code yyyymmdd}가 {@code baseYyyymmdd}보다 이전이면 음수).
     *
     * <p>부호(-1 / 0 / +1)만 필요한 호출자는 결과를 {@link Integer#signum(int)}로
     * 감싸서 사용할 수 있다.
     */
    public static int diffDays(String yyyymmdd, String baseYyyymmdd) {
        LocalDate d = LocalDate.of(
                Integer.parseInt(yyyymmdd.substring(0, 4)),
                Integer.parseInt(yyyymmdd.substring(4, 6)),
                Integer.parseInt(yyyymmdd.substring(6, 8)));
        LocalDate b = LocalDate.of(
                Integer.parseInt(baseYyyymmdd.substring(0, 4)),
                Integer.parseInt(baseYyyymmdd.substring(4, 6)),
                Integer.parseInt(baseYyyymmdd.substring(6, 8)));
        return (int) ChronoUnit.DAYS.between(b, d);
    }

    /**
     * (yyyyMMdd, HHmm) 쌍을 workYmd 기준 분 stamp로 변환한다.
     * 파싱 실패나 범위 벗어남 시 null을 반환한다.
     *
     * <p>{@link NumberFormatException}(숫자가 아닌 문자)과
     * {@link DateTimeException}(예: month=13, day=32) 모두 catch 하므로
     * 잘못된 날짜 입력이 500 에러로 누출되지 않는다.
     */
    public static Integer toMinuteStamp(String workYmd, String date, String time) {
    	if (workYmd == null || workYmd.length() != 8) return null;
        if (date == null || date.length() != 8) return null;
        if (time == null || time.length() != 4) return null;
        try {
            int hh = Integer.parseInt(time.substring(0, 2));
            int mm = Integer.parseInt(time.substring(2, 4));
            if (hh < 0 || hh > 23) return null;
            if (mm < 0 || mm > 59) return null;
            int dayOffset = diffDays(date, workYmd);
            // 기준점을 workYmd 전날 00:00으로 이동 (야간 교대 지원: 전날 ~ 당일 ~ 다음날 = 48시간 윈도우)
            return Integer.valueOf((dayOffset + 1) * 1440 + hh * 60 + mm);
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    /**
     * workYmd 기준 [startStamp, endStamp] 분 stamp 범위를 빌드한다.
     *
     * <p>다음 경우 null을 반환한다:
     * <ul>
     *   <li>endpoint(시작/종료) 중 하나라도 파싱 실패,</li>
     *   <li>end가 start보다 엄격히 크지 않은 경우,</li>
     *   <li>또는 범위가 지원 윈도우 0..4320(workYmd-1 00:00 ~ workYmd+1 23:59)을
     *       벗어나는 경우. [QA 재작업 D1] 오버나이트 OT 요청/실근무가 workYmd+1로
     *       넘어가는 케이스를 지원하기 위해 상한을 2880에서 4320으로 확장.</li>
     * </ul>
     */
    public static int[] toStampRange(String workYmd,
                                     String sDate, String sTime,
                                     String eDate, String eTime) {
        Integer sStamp = toMinuteStamp(workYmd, sDate, sTime);
        Integer eStamp = toMinuteStamp(workYmd, eDate, eTime);
        if (sStamp == null || eStamp == null) {
            return null;
        }
        if (eStamp <= sStamp) {
            return null;
        }
        if (sStamp < 0 || eStamp > 4320) {
            return null;
        }
        return new int[] { sStamp.intValue(), eStamp.intValue() };
    }

    /**
     * 길이 8의 yyyyMMdd 문자열에서 {@code months} 개월을 뺀 yyyyMMdd 문자열을 반환한다.
     * 말일 보정은 {@link LocalDate#minusMonths(long)} 규칙을 따른다(예: 3/31 - 1개월 = 2/28).
     * 입력이 null이거나 형식이 잘못된 경우 null을 반환한다.
     */
    public static String minusMonths(String yyyymmdd, int months) {
        LocalDate d = parseYyyymmdd(yyyymmdd);
        if (d == null) return null;
        LocalDate r = d.minusMonths(months);
        return String.format("%04d%02d%02d", r.getYear(), r.getMonthValue(), r.getDayOfMonth());
    }

    /**
     * 길이 8의 yyyyMMdd 문자열을 yyyy-MM-dd로 변환한다.
     * 입력이 null/공백/형식 오류면 빈 문자열을 반환한다(화면 표기 안전 기본값).
     */
    public static String toHyphenDate(String yyyymmdd) {
        LocalDate d = parseYyyymmdd(yyyymmdd);
        if (d == null) return "";
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    /**
     * 길이 8의 yyyyMMdd 문자열을 LocalDate로 파싱한다. 실패 시 null.
     */
    public static LocalDate parseYyyymmdd(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return null;
        try {
            return LocalDate.of(
                    Integer.parseInt(yyyymmdd.substring(0, 4)),
                    Integer.parseInt(yyyymmdd.substring(4, 6)),
                    Integer.parseInt(yyyymmdd.substring(6, 8)));
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
    }
}
