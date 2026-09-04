package com.prafta.common.cmm.sch.util;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.DateTimeUtils;

/**
 * BW-10(2026-09-04, 요청서 §3 G-6): 근무타입 휴게 시각 저장 검증 — <b>Attd_01 저장 · Platform_01 프로비저닝 공용</b>(순수 함수).
 *
 * <p>종전 {@code Attd01ServiceImpl.validateBreakTimeRangeForSegment}(F-2, 2026-08-08)의 3규칙을 그대로 옮기고
 * G-6(휴게 시각 폭 ≠ 휴게분 거부)을 더했다. 위반은 전부 {@code ATTD_400_197} + detailMessage(구체 사유).
 * <ol>
 *   <li>휴게분 ≤ 근무시간(오버나이트 span)</li>
 *   <li>휴게 시작 ∈ 근무 범위(오버나이트 [start,24:00)∪[00:00,end))</li>
 *   <li>휴게 시작 + 휴게분 ≤ 근무 종료(갭2)</li>
 *   <li>G-6: 휴게 종료가 있으면 {@code (종료 − 시작) mod 1440 == 휴게분}. 종료 파싱은
 *       {@link DateTimeUtils#brkEndToMinutes}('2400'=1440) + 종료 &lt; 시작이면 자정 넘김 wrap.</li>
 * </ol>
 * 휴게 시작이 NULL/빈값이면 검증 생략(휴게는 선택 항목 — 정책서 attd §3.2, 기존 운영 행 보존).
 * 근무 시각 자체가 비정상이면(결손/시작==종료) 이 검증에서는 판단하지 않는다(다른 검증 담당).
 *
 * <p>종료 미전송 시 저장값 파생은 {@link #deriveBreakEnd}(시작 + 휴게분, 자정 넘김은 HHMM wrap — 보정 SQL 규약과 동일).
 */
public final class BreakTimeValidator {

    private static final int MINUTES_PER_DAY = 1440;

    private BreakTimeValidator() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 1·2구간 일괄 검증(schType '02' 일 때만 2구간). */
    public static void validate(String schType,
                                String fstSchStrTime, String fstSchEndTime, String fstSchBrkMin,
                                String fstBrkStrTime, String fstBrkEndTime,
                                String secSchStrTime, String secSchEndTime, String secSchBrkMin,
                                String secBrkStrTime, String secBrkEndTime) {
        validateSegment("구간1", fstSchStrTime, fstSchEndTime, fstSchBrkMin, fstBrkStrTime, fstBrkEndTime);
        if ("02".equals(schType)) {
            validateSegment("구간2", secSchStrTime, secSchEndTime, secSchBrkMin, secBrkStrTime, secBrkEndTime);
        }
    }

    /**
     * 구간 1개(근무시간 + 휴게) 검증.
     *
     * @param segmentLabel detail 문구 접두("구간1"/"구간2")
     * @param schStrTime   근무 시작 "HHmm"/"HH:mm"
     * @param schEndTime   근무 종료 "HHmm"/"HH:mm"/"2400"
     * @param schBrkMin    휴게(분) 문자열(null/비정상 = 0)
     * @param brkStrTime   휴게 시작(null/빈값 = 검증 생략)
     * @param brkEndTime   휴게 종료(null/빈값 = G-6 생략, 종료 파생은 호출부 {@link #deriveBreakEnd})
     */
    public static void validateSegment(String segmentLabel,
                                       String schStrTime, String schEndTime, String schBrkMin,
                                       String brkStrTime, String brkEndTime) {
        Integer brkStart = toMinutes(brkStrTime);
        if (brkStart == null) {
            return; // 휴게 시각 없음 — NULL 허용
        }
        Integer start = toMinutes(schStrTime);
        Integer end = toMinutes(schEndTime);
        if (start == null || end == null || start.equals(end)) {
            return; // 근무시간 비정상은 이 검증의 판단 대상이 아니다
        }
        int brk = parseBreakMin(schBrkMin);
        int workMin = spanMinutes(start, end);

        if (brk > workMin) {
            throw new ApiException(AttdErrorCode.ATTD_400_197,
                    segmentLabel + " 휴게시간은 근무시간(" + workMin + "분)보다 많을 수 없습니다.");
        }
        if (!withinSpan(start, end, brkStart)) {
            throw new ApiException(AttdErrorCode.ATTD_400_197,
                    segmentLabel + " 휴게시간 시작 시각은 근무시간 범위 안이어야 합니다.");
        }
        int brkOffset = (brkStart - start + MINUTES_PER_DAY) % MINUTES_PER_DAY;
        if (brkOffset + brk > workMin) {
            throw new ApiException(AttdErrorCode.ATTD_400_197,
                    segmentLabel + " 휴게시간 종료 시각이 근무 종료 시각을 초과합니다.");
        }

        // G-6: 휴게 시각 폭 == 휴게분 (종료가 실려 온 경우만).
        //   ★ 파싱은 본 클래스의 toMinutes 로 한다 — 프론트(SchInfoPop)는 파생 종료를 "HH:mm"(콜론 포함)으로
        //   보내므로 "HHmm" 4자리만 받는 DateTimeUtils.brkEndToMinutes 로는 전부 null 이 되어
        //   "형식이 올바르지 않습니다" 로 근무타입 저장이 전면 실패한다(2026-09-04 QA 실기동 적발).
        Integer brkEnd = toMinutes(blankToNull(brkEndTime));
        if (brkEnd == null) {
            if (brkEndTime != null && !brkEndTime.isBlank()) {
                throw new ApiException(AttdErrorCode.ATTD_400_197,
                        segmentLabel + " 휴게 종료 시각 형식이 올바르지 않습니다.");
            }
            return; // 종료 미전송 — 호출부가 시작 + 휴게분으로 파생 저장
        }
        int width = ((brkEnd - brkStart) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY;
        if (width != brk) {
            throw new ApiException(AttdErrorCode.ATTD_400_197,
                    segmentLabel + " 휴게 시각 폭(" + width + "분)이 휴게시간(" + brk + "분)과 다릅니다. "
                            + "휴게 시작 시각 또는 휴게시간을 확인해 주세요.");
        }
    }

    /**
     * 휴게 종료 파생값(HHMM) = 시작 + 휴게분. 자정을 넘기면 HHMM wrap(예: '2330'+60 → '0030' — 보정 SQL 규약).
     * 시작이 없거나 파싱 불가면 null(저장 NULL 유지).
     */
    public static String deriveBreakEnd(String brkStrTime, String schBrkMin) {
        Integer brkStart = toMinutes(brkStrTime);
        if (brkStart == null) {
            return null;
        }
        int end = (brkStart + parseBreakMin(schBrkMin)) % MINUTES_PER_DAY;
        return String.format("%02d%02d", end / 60, end % 60);
    }

    /** "HH:mm" 또는 "HHmm" → 0~1440(24:00 포함) 분. 형식 오류/미입력 시 null. */
    /**
     * 입력을 저장용 HHMM 4자리로 정규화한다("HH:mm" 허용). 24:00 은 "2400". 파싱 불가면 null.
     */
    public static String normalizeHhmm(String v) {
        Integer min = toMinutes(v);
        if (min == null) {
            return null;
        }
        if (min == MINUTES_PER_DAY) {
            return "2400";
        }
        return String.format("%02d%02d", min / 60, min % 60);
    }

    static Integer toMinutes(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim().replaceAll("\\D", "");
        if (s.length() < 4) {
            return null;
        }
        int h;
        int m;
        try {
            h = Integer.parseInt(s.substring(0, 2));
            m = Integer.parseInt(s.substring(2, 4));
        } catch (NumberFormatException e) {
            return null;
        }
        if (h == 24 && m == 0) {
            return MINUTES_PER_DAY;
        }
        if (h < 0 || h > 23 || m < 0 || m > 59) {
            return null;
        }
        return h * 60 + m;
    }

    /** 휴게(분) 문자열 파싱. 미입력/파싱 불가 시 0. */
    static int parseBreakMin(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 오버나이트 고려 근무 길이(분): 종료 ≤ 시작이면 자정을 넘긴 것으로 보고 1440 을 더한다. */
    private static int spanMinutes(int start, int end) {
        return end > start ? end - start : end + MINUTES_PER_DAY - start;
    }

    /** 오버나이트 고려 포함 여부: t 가 [start, end) 구간(자정 넘김 포함) 안인지. */
    private static boolean withinSpan(int start, int end, int t) {
        if (end > start) {
            return t >= start && t < end;
        }
        return t >= start || t < end;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
