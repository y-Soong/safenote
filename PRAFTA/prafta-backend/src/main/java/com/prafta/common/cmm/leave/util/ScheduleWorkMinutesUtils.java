package com.prafta.common.cmm.leave.util;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.util.DateTimeUtils;

/**
 * 근무 스케줄 소정근로분 계산 순수 함수 (개인 분모 개편 PC-03 — 단일 출처).
 *
 * <p>기존 {@code LeaveDeductionServiceImpl}의 private 구간 계산(segmentWorkMinutes)을 공용 추출했다.
 * <ul>
 *   <li>그날 소정근로분 D(마일스톤 하한 기준 — {@code getDailyStdWorkMinutes})와</li>
 *   <li>실차감 분모 conv(당일 배정 스케줄 소정근로분 — {@code resolveDailyConvMinutes}, E1)와</li>
 *   <li>E4 참고 분모(기본 근무타입 소정근로분 — {@code resolvePersonalConvMinutes}.
 *       2026-08-09 표기 규약 변경 이후 내부 판정·구버전 호환용, 신 FE 표기 미사용)가</li>
 * </ul>
 * 반드시 같은 산식을 공유하도록 본 클래스를 단일 출처로 사용한다.
 *
 * <p>산식: 각 구간 근로분 = {@code (종료 − 시작) − 휴게(분)} (종료 ≤ 시작이면 야간 구간 +1440 보정),
 * 1구간 필수 + 2구간 선택 합산. 도메인 종속(연차/스케줄) 유틸이므로 common.util 이 아닌 모듈 내부에 둔다.
 *
 * <p>★PRAFTA-FIXEDOT-2 가드: 연차 분모(D/E1)는 <b>소정근로만</b>이다 — 고정연장근무
 * (PRE_FIXED_OT_ / FIXED_OT_ 계열 컬럼) 분을 여기에 절대 합산하지 말 것(480 캡 정합 — 지시서 지점 2).
 */
public final class ScheduleWorkMinutesUtils {

    /** 하루(분). 야간 구간(종료 ≤ 시작) 보정에 사용. */
    private static final int MINUTES_PER_DAY = 1440;

    private ScheduleWorkMinutesUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 스케줄의 1일 소정근로분 = 1구간 + (선택) 2구간 근로분 합산.
     *
     * @return 소정근로분(양수). 스케줄 null/1구간 비정상/합계 0 이하면 {@code null}.
     */
    public static Integer dailyStdWorkMinutes(DailyScheduleVO sch) {
        if (sch == null) {
            return null;
        }
        Integer fst = segmentWorkMinutes(sch.getFstSchStrTime(), sch.getFstSchEndTime(), sch.getFstSchBrkMin());
        if (fst == null) {
            // 1구간이 비정상이면 전체 산출 불가 (1구간은 필수)
            return null;
        }
        int total = fst;
        // 2구간은 선택적 — 시작/종료가 모두 있을 때만 합산
        if (sch.getSecSchStrTime() != null && !sch.getSecSchStrTime().isBlank()
                && sch.getSecSchEndTime() != null && !sch.getSecSchEndTime().isBlank()) {
            Integer sec = segmentWorkMinutes(sch.getSecSchStrTime(), sch.getSecSchEndTime(), sch.getSecSchBrkMin());
            if (sec != null) {
                total += sec;
            }
        }
        return total > 0 ? Integer.valueOf(total) : null;
    }

    /**
     * 한 구간의 근로분 = {@code (종료 - 시작) - 휴게(분)}. 종료 ≤ 시작이면 야간 구간으로 보아 +1440 보정.
     * 시각 파싱 실패나 음수 결과 시 {@code null}.
     */
    public static Integer segmentWorkMinutes(String strTime, String endTime, String brkMin) {
        Integer start = DateTimeUtils.hhmmToMinutes(strTime);
        // 종료는 자정 경계 근무(정책 attd/03 §3.3) "2400"=1440 을 인정하는 종료 전용 파서 사용.
        Integer end = DateTimeUtils.schEndToMinutes(endTime);
        if (start == null || end == null) {
            return null;
        }

        int span = end - start;
        if (span <= 0) {
            // 야간 구간 (예: 22:00 ~ 06:00)
            span += MINUTES_PER_DAY;
        }

        int brk = parseBrkMin(brkMin);
        int work = span - brk;
        return work > 0 ? Integer.valueOf(work) : null;
    }

    // ================================================================
    // HB-01: 반차 경계 산출 (반차 시간대 도입, 2026-08-07)
    //   정책서 attd/08-leave.md §8.5.10(2026-09-04 신설) — (a) 반차 경계 원칙이 본 산식의 조문.
    //   D = Σ(구간 종료 − 시작 − 휴게), H = D / 2 (정수 절사 — 차감 LEAVE_MINUTES 와 동일 연산자),
    //   segs  = 근무 구간 목록(1구간 + 선택 2구간, 자정 넘김 +1440),
    //   parts = 휴게 시각을 제외한 근로 조각(휴게 분만 있으면 span−brkMin 단일 조각).
    //
    // 부분휴가 휴게 무시 도입(2026-09-04, 요청서 §1-1 / plan BW-02) — 파트·체크 4조합:
    //   일찍 퇴근(END)  미체크: 시작에서 parts 를 걸어 H 도달 시각            (현행 동일 — 무회귀 축)
    //                  체크  : 시작에서 segs  를 걸어 H 도달 시각            (휴게를 건너뛰지 않음)
    //   늦게 출근(START) 미체크: 종료에서 거꾸로 parts 를 걸어 H 도달 시각     (★G-3 — 종전 "시작 걷기 공용"에서 변경)
    //                  체크  : 종료에서 거꾸로 segs  를 걸어 H 도달 시각
    //   체크 결과가 미체크와 같으면(휴게가 쉬는 구간 안) 시각 불변 + 기록 전용(recordOnly).
    //   휴게 분만 등록(시각 없음)이면 위치 미상 → 체크해도 시각 불변, 기록 전용(G-2).
    //   기대값 출처: .claude/refs/휴게무시_시뮬레이션/brk_sim.py (simulate / walk_from_start / walk_from_end /
    //   walk_skip_from_end) — 분만 타입 START 체크 열만 G-2 적용 전이라 제외.
    // ================================================================

    /** 반차 파트. START = 늦게 출근(쉬는 구간 [시작, 경계)), END = 일찍 퇴근(쉬는 구간 [경계, 종료)). */
    public enum HalfPart {
        START, END;

        /** 문자열("START"/"END", 대소문자·공백 무관) → 파트. 그 외/null 은 {@code null}(호출부 fail-closed 거부). */
        public static HalfPart of(String raw) {
            if (raw == null) {
                return null;
            }
            String v = raw.trim().toUpperCase();
            if ("START".equals(v)) {
                return START;
            }
            if ("END".equals(v)) {
                return END;
            }
            return null;
        }
    }

    /**
     * 반차 경계 산출 결과. 모든 분 값은 <b>근무일 00:00 = 0 기준</b>이며, 자정을 넘기는 야간 근무는
     * 1440 을 넘는 값으로 표현된다(예: 익일 01:15 = 1515). 저장용 HHMM 은 {@link #hhmmOfDay(int)} 로 얻는다
     * (★ Q5 정정: 저장 일자는 항상 근무일 — 자정 넘김은 {@code END_TIME < START_TIME} 시각 wrap 으로 표현).
     *
     * @param boundaryMin          경계 시각(분)
     * @param workStartMin         근무 시작 시각(분, 1구간 시작)
     * @param workEndMin           근무 종료 시각(분, 마지막 구간 종료)
     * @param exemptMinutes        반차 면제 근로분 H( = D / 2 정수 절사, 차감 LEAVE_MINUTES 와 동일 값)
     * @param part                 산출 기준 파트. 4-인자 생성자(구 호출부)는 END.
     * @param waiveRequested       휴게 무시 체크 요청 여부(입력 echo)
     * @param recordOnly           체크 요청했지만 경계가 미체크와 같아 시각 불변·요청 기록만 하는 경우 {@code true}
     *                             (휴게가 쉬는 구간 안 / 휴게 분만 등록 G-2). 미체크 요청이면 항상 {@code false}.
     * @param breakTimeRegistered  그날 스케줄에 휴게 <b>시각</b> 구간이 1개 이상 있으면 {@code true}(G-2 안내용)
     */
    public record HalfDayBoundary(int boundaryMin, int workStartMin, int workEndMin, int exemptMinutes,
                                  HalfPart part, boolean waiveRequested, boolean recordOnly,
                                  boolean breakTimeRegistered) {

        /** 구 시그니처 호환 생성자 — END·미체크 의미(기존 호출부 무회귀). */
        public HalfDayBoundary(int boundaryMin, int workStartMin, int workEndMin, int exemptMinutes) {
            this(boundaryMin, workStartMin, workEndMin, exemptMinutes, HalfPart.END, false, false, false);
        }

        /** 파트 기준 쉬는(면제) 구간 시작(분): END = 경계, START = 근무 시작. */
        public int exemptStartMin() {
            return part == HalfPart.START ? workStartMin : boundaryMin;
        }

        /** 파트 기준 쉬는(면제) 구간 종료(분): END = 근무 종료, START = 경계. */
        public int exemptEndMin() {
            return part == HalfPart.START ? boundaryMin : workEndMin;
        }
    }

    /**
     * 그날 스케줄의 반차 경계를 산출한다(순수 함수) — <b>END(일찍 퇴근)·미체크</b> 기준.
     *
     * <p>{@link #halfDayBoundary(DailyScheduleVO, HalfPart, boolean)} 의 {@code (sch, END, false)} 위임이며
     * 결과 경계는 종전 산식과 바이트 동일하다(무회귀 축 — 요청서 §4, 시뮬 불변식 I1 위반 0).
     * 기존 호출부(앱/웹 submit·preview·day-schedule, Attd_13 이동)는 이 결과의 {@code boundaryMin} 을
     * START/END 양 파트에 공용으로 써 왔다 — 파트별 경계(G-3)가 필요한 호출부는 새 오버로드를 쓴다.
     *
     * @return 경계 산출 결과. 스케줄 없음/시각 비정상/D&lt;2 면 {@code null}.
     */
    public static HalfDayBoundary halfDayBoundary(DailyScheduleVO sch) {
        return halfDayBoundary(sch, HalfPart.END, false);
    }

    /**
     * 그날 스케줄의 반차 경계를 파트·휴게 무시 체크 여부로 산출한다(순수 함수).
     *
     * <p>산출 규칙(요청서 §1-1 / plan BW-02, 상단 블록 주석의 4조합):
     * <ol>
     *   <li>{@code D = dailyStdWorkMinutes(sch)}. {@code null} 이거나 2 미만이면 {@code null}(호출부 거부).</li>
     *   <li>{@code H = D / 2} — <b>정수 절사</b>. 반올림·5분 스냅 금지(차감 {@code leaveMinutes = daily/2} 와 동일해야 함).</li>
     *   <li>미체크 경계: END 는 시작에서 근로 조각(parts)을 걸어 H 도달, START 는 종료에서 거꾸로 parts 를 걸어 H 도달.
     *       휴게 <b>시각</b>이 있으면 그 구간을 건너뛴다(근무 구간과 교집합 클램프 — 근무 밖 휴게 방어).
     *       휴게 시각이 없고 분만 있으면 조각 기여를 {@code span - brkMin} 으로 제한한다(운영 최다 패턴).</li>
     *   <li>체크 경계: 휴게 시각 구간이 없으면(분만/미등록) 시각 불변·{@code recordOnly=true}(G-2).
     *       있으면 END 는 시작에서 근무 구간(segs)을, START 는 종료에서 거꾸로 segs 를 걸어 H 도달 시각.
     *       결과가 미체크 경계와 같으면 시각 불변·{@code recordOnly=true}.</li>
     * </ol>
     *
     * @param sch   그날 스케줄(1구간 필수)
     * @param part  반차 파트. {@code null} 이면 END 로 본다.
     * @param waive 휴게 무시 체크 여부
     * @return 경계 산출 결과. 스케줄 없음/시각 비정상/D&lt;2 면 {@code null}.
     */
    public static HalfDayBoundary halfDayBoundary(DailyScheduleVO sch, HalfPart part, boolean waive) {
        HalfPart p = (part == null) ? HalfPart.END : part;
        Integer d = dailyStdWorkMinutes(sch);
        if (d == null || d < 2) {
            return null;
        }
        // ★ 차감 leaveMinutes = daily / 2 와 동일 연산자(정수 절사). 두 값이 갈라지면 안 된다.
        int h = d / 2;

        // 구간 경계(근무 시작/종료)와 근로 조각(경계 누적용)을 따로 만든다.
        //   ★ 근무 종료는 반드시 "마지막 구간의 종료"여야 한다. 휴게 분만 있고 시각이 없는 스케줄에서는
        //     근로 조각이 구간 끝보다 brkMin 만큼 일찍 끝나므로(예: 09:00~18:00 brk60 → 조각 09:00~17:00),
        //     조각 끝을 근무 종료로 쓰면 종료기준 반차가 [13:00, 17:00] 로 잘못 저장된다.
        List<int[]> segments = buildSegments(sch);
        if (segments.isEmpty()) {
            return null;
        }
        List<int[]> workParts = buildWorkParts(sch, segments);
        if (workParts.isEmpty()) {
            return null;
        }

        int workStart = segments.get(0)[0];
        int workEnd = segments.get(segments.size() - 1)[1];

        // 방어: 휴게 시각 폭이 brkMin 보다 크면 누적 가능분이 D 보다 작을 수 있다(데이터 불일치).
        //   실무상 H(=D/2) 를 못 채우는 경우는 없으나, 못 채우면 마지막 근로 종료(START 는 첫 근로 시작)로
        //   클램프한다(null 을 반환해 기존에 되던 반차 신청이 막히는 회귀를 만들지 않는다).
        int target = Math.min(h, totalLength(workParts));
        int plain = (p == HalfPart.END)
                ? walkFromStart(workParts, target)
                : walkFromEnd(workParts, target);

        // 휴게 시각 구간(근무 구간 프레임·클램프) — 체크 시 이동 가능 여부(G-2)와 recordOnly 판정 근거.
        List<int[]> breakTimes = breakIntervals(sch, segments);
        boolean breakTimeRegistered = !breakTimes.isEmpty();

        int boundary = plain;
        boolean recordOnly = false;
        if (waive) {
            if (!breakTimeRegistered) {
                // G-2: 휴게 분만 등록(위치 미상) 또는 휴게 없음 — 시각 불변, 요청 기록만.
                recordOnly = true;
            } else {
                // 체크: 휴게를 건너뛰지 않고 근무 구간(segs) 자체를 걷는다. 구간 사이 공백은 건너뛴다(G-4).
                int segTarget = Math.min(h, totalLength(segments));
                int checked = (p == HalfPart.END)
                        ? walkFromStart(segments, segTarget)
                        : walkFromEnd(segments, segTarget);
                if (checked == plain) {
                    // 휴게가 쉬는 구간 안(근무 쪽에 휴게 없음) — 결과 동일 → 기록 전용.
                    recordOnly = true;
                } else {
                    boundary = checked;
                }
            }
        }
        return new HalfDayBoundary(boundary, workStart, workEnd, h, p, waive, recordOnly, breakTimeRegistered);
    }

    // ================================================================
    // BW-06(2026-09-04): 법정 휴게 하한 경고(BreakLegalCheckUtils)가 같은 프레임의 구간을 쓰도록 공개 접근자.
    //   (산식 단일 출처 — 별도 파서 재구현 금지. 반환 목록은 새 리스트라 호출부가 변형해도 무해.)
    // ================================================================

    /** 근무 구간 목록(근무일 00:00 = 0 기준, 야간 1440 초과). 1구간 없으면 빈 목록. */
    public static List<int[]> workSegments(DailyScheduleVO sch) {
        return buildSegments(sch);
    }

    /** 근로 조각 목록(휴게 시각 제외, 분만이면 span − brkMin 단일 조각). 1구간 없으면 빈 목록. */
    public static List<int[]> workParts(DailyScheduleVO sch) {
        List<int[]> segments = buildSegments(sch);
        if (segments.isEmpty()) {
            return new ArrayList<>();
        }
        return buildWorkParts(sch, segments);
    }

    /** 휴게 시각 구간 목록(근무 구간 프레임·클램프). 분만 등록/미등록이면 빈 목록. */
    public static List<int[]> breakTimeIntervals(DailyScheduleVO sch) {
        return breakIntervals(sch, buildSegments(sch));
    }

    /** 스케줄 휴게 분 합(1구간 + 2구간, 파싱 실패/음수는 0). */
    public static int totalBreakMinutes(DailyScheduleVO sch) {
        if (sch == null) {
            return 0;
        }
        return parseBrkMin(sch.getFstSchBrkMin()) + parseBrkMin(sch.getSecSchBrkMin());
    }

    /** 구간 목록의 길이 합(분). */
    private static int totalLength(List<int[]> ranges) {
        int total = 0;
        for (int[] r : ranges) {
            total += r[1] - r[0];
        }
        return total;
    }

    /**
     * 구간 목록을 앞에서부터 걸어 {@code target} 분 누적에 도달하는 시각. 못 채우면 마지막 구간 종료.
     * (brk_sim {@code walk_from_start} — parts 를 주면 미체크 END, segs 를 주면 체크 END.)
     */
    private static int walkFromStart(List<int[]> ranges, int target) {
        int acc = 0;
        for (int[] r : ranges) {
            int len = r[1] - r[0];
            if (acc + len >= target) {
                return r[0] + (target - acc);
            }
            acc += len;
        }
        return ranges.get(ranges.size() - 1)[1];
    }

    /**
     * 구간 목록을 뒤에서부터 거꾸로 걸어 {@code target} 분 누적에 도달하는 시각. 못 채우면 첫 구간 시작.
     * (brk_sim {@code walk_skip_from_end}(parts, 미체크 START) / {@code walk_from_end}(segs, 체크 START).)
     */
    private static int walkFromEnd(List<int[]> ranges, int target) {
        int acc = 0;
        for (int i = ranges.size() - 1; i >= 0; i--) {
            int[] r = ranges.get(i);
            int len = r[1] - r[0];
            if (acc + len >= target) {
                return r[1] - (target - acc);
            }
            acc += len;
        }
        return ranges.get(0)[0];
    }

    /**
     * 스케줄의 근무 구간 목록(근무일 00:00 = 0 기준, 야간은 1440 초과 값). 1구간 필수 + 2구간 선택.
     * 2구간 시작이 1구간 종료보다 이르면 자정을 넘긴 것으로 보고 하루 뒤로 민다(야간 2구간).
     */
    private static List<int[]> buildSegments(DailyScheduleVO sch) {
        List<int[]> segments = new ArrayList<>(2);
        if (sch == null) {
            return segments;
        }
        int[] seg1 = segmentRange(sch.getFstSchStrTime(), sch.getFstSchEndTime(), null);
        if (seg1 == null) {
            // 1구간은 필수 — 없으면 산출 불가.
            return segments;
        }
        segments.add(seg1);

        if (sch.getSecSchStrTime() != null && !sch.getSecSchStrTime().isBlank()
                && sch.getSecSchEndTime() != null && !sch.getSecSchEndTime().isBlank()) {
            int[] seg2 = segmentRange(sch.getSecSchStrTime(), sch.getSecSchEndTime(), seg1[1]);
            if (seg2 != null) {
                segments.add(seg2);
            }
        }
        return segments;
    }

    /**
     * 스케줄의 "실제 근로" 하위 구간 목록(경계 누적용). 휴게 시각이 있으면 그 구간을 제외한 앞/뒤 조각으로,
     * 없으면 {@code span - brkMin} 길이의 단일 조각으로 만든다.
     */
    private static List<int[]> buildWorkParts(DailyScheduleVO sch, List<int[]> segments) {
        List<int[]> parts = new ArrayList<>(4);
        if (!segments.isEmpty()) {
            addWorkParts(parts, segments.get(0),
                    sch.getFstBrkStrTime(), sch.getFstBrkEndTime(), sch.getFstSchBrkMin());
        }
        if (segments.size() > 1) {
            addWorkParts(parts, segments.get(1),
                    sch.getSecBrkStrTime(), sch.getSecBrkEndTime(), sch.getSecSchBrkMin());
        }
        return parts;
    }

    /**
     * 한 구간의 [시작, 종료) 분 범위. 종료 ≤ 시작이면 야간으로 보아 +1440.
     * {@code minStart} 가 주어지면(2구간) 시작이 그보다 이를 때 하루 뒤로 민다.
     */
    private static int[] segmentRange(String strTime, String endTime, Integer minStart) {
        Integer s = DateTimeUtils.hhmmToMinutes(strTime);
        // 종료는 자정 경계 근무("2400"=1440) 를 인정하는 종료 전용 파서 사용(기존 규약 승계).
        Integer e = DateTimeUtils.schEndToMinutes(endTime);
        if (s == null || e == null) {
            return null;
        }
        int start = s;
        if (minStart != null) {
            while (start < minStart) {
                start += MINUTES_PER_DAY;
            }
        }
        int end = e;
        while (end <= start) {
            end += MINUTES_PER_DAY;
        }
        return new int[] { start, end };
    }

    /** 한 구간을 근로 조각으로 분해해 {@code parts} 에 추가한다. */
    private static void addWorkParts(List<int[]> parts, int[] seg, String brkStr, String brkEnd, String brkMin) {
        int segStart = seg[0];
        int segEnd = seg[1];
        int span = segEnd - segStart;
        int brk = parseBrkMin(brkMin);

        int[] bi = breakInterval(seg, brkStr, brkEnd);
        if (bi != null) {
            addIfPositive(parts, segStart, bi[0]);
            addIfPositive(parts, bi[1], segEnd);
            return;
        }

        // 휴게 시각 미설정(운영 최다 패턴) — 건너뛸 구간 없이 연속 누적하되 기여를 span-brkMin 으로 제한.
        int segWork = span - brk;
        if (segWork > 0) {
            addIfPositive(parts, segStart, segStart + segWork);
        }
    }

    /**
     * 한 구간의 휴게 <b>시각</b> 구간을 구간 프레임으로 이동·클램프해 {@code [시작, 종료)} 로 돌려준다.
     * 휴게 시각 미설정/파싱 실패/0폭이거나 근무 구간과 겹치지 않으면 {@code null}.
     *
     * <p>G-1(2026-09-04): 휴게 종료는 {@link DateTimeUtils#brkEndToMinutes} 로 파싱해 {@code "2400"}=1440 을
     * 인정하고, 종료 &lt; 시작이면 자정 넘김 휴게(예: 23:30~00:30 → 보정 저장값 '2330'~'0030')로 보아 +1440
     * wrap 한다. 종료 == 시작(예: '0000'~'0000')은 0폭 = 휴게 시각 없음(운영 행 보존).
     * 그 뒤 구간 프레임 이동({@code while (bs < segStart)})은 종전 로직 그대로다.
     */
    private static int[] breakInterval(int[] seg, String brkStr, String brkEnd) {
        Integer rbs = DateTimeUtils.hhmmToMinutes(brkStr);
        Integer rbe = DateTimeUtils.brkEndToMinutes(brkEnd);
        if (rbs == null || rbe == null) {
            return null;
        }
        int bs = rbs;
        int be = rbe;
        if (be < bs) {
            // 자정 넘김 휴게 — 익일 wrap.
            be += MINUTES_PER_DAY;
        }
        if (be == bs) {
            // 0폭 = 휴게 시각 없음.
            return null;
        }
        int segStart = seg[0];
        int segEnd = seg[1];
        // 휴게 시각을 구간과 같은 날짜 프레임으로 이동(야간 구간 지원).
        while (bs < segStart) {
            bs += MINUTES_PER_DAY;
            be += MINUTES_PER_DAY;
        }
        // 근무 구간과 교집합 클램프 — 근무 밖에 저장된 휴게(F-2 갭2)는 무시된다.
        int cbs = Math.max(bs, segStart);
        int cbe = Math.min(be, segEnd);
        if (cbe > cbs) {
            return new int[] { cbs, cbe };
        }
        return null;
    }

    /**
     * 스케줄의 휴게 <b>시각</b> 구간 목록(근무 구간 프레임·클램프, 1구간 → 2구간 순). 분만 등록/미등록이면 빈 목록.
     * 반차 체크 경계(G-2 판정)와 시간차 붙은 휴게 편입({@link #mergeAdjacentBreaks})이 공용한다.
     */
    private static List<int[]> breakIntervals(DailyScheduleVO sch, List<int[]> segments) {
        List<int[]> out = new ArrayList<>(2);
        if (sch == null || segments.isEmpty()) {
            return out;
        }
        int[] b1 = breakInterval(segments.get(0), sch.getFstBrkStrTime(), sch.getFstBrkEndTime());
        if (b1 != null) {
            out.add(b1);
        }
        if (segments.size() > 1) {
            int[] b2 = breakInterval(segments.get(1), sch.getSecBrkStrTime(), sch.getSecBrkEndTime());
            if (b2 != null) {
                out.add(b2);
            }
        }
        return out;
    }

    private static void addIfPositive(List<int[]> parts, int start, int end) {
        if (end > start) {
            parts.add(new int[] { start, end });
        }
    }

    // ================================================================
    // BW-03: 시간차 "붙은 휴게 편입" 규칙 (부분휴가 휴게 무시 도입, 2026-09-04 — 요청서 §1-2)
    //   체크 시 신청 구간 [s, e) 에 접하거나 겹치는 휴게 시각 구간을 쉬는 구간에 합친다(반복 병합).
    //   저장 시각 = 합친 구간, 차감 분 = (e − s) − (신청 ∩ 휴게 겹침). 합친 결과가 신청과 같으면 기록 전용.
    //   휴게 시각이 없는 타입(분만/미등록)은 편입할 구간이 없어 기록 전용(G-2).
    //   미체크 경로(가로지름 거부 ATTD_400_055)는 본 함수를 타지 않는다(호출부 분기는 BW-04).
    // ================================================================

    /**
     * 시간차 휴게 편입 결과. 분 값은 입력 {@code startMin/endMin} 과 <b>같은 프레임</b>(호출부가 넘긴 원시 분).
     *
     * @param exemptStartMin       저장할 쉬는 구간 시작(분) — 편입 후
     * @param exemptEndMin         저장할 쉬는 구간 종료(분) — 편입 후
     * @param requestMinutes       신청 길이 {@code endMin - startMin}(단위 배수 검증 대상 — Q-10 신청 길이 기준 유지)
     * @param chargeMinutes        차감 분 = 신청 길이 − (신청 ∩ 휴게 겹침). {@code LEAVE_MINUTES}·{@code calcHourlyCharge} 입력
     * @param overlapBreakMinutes  신청 구간과 휴게 시각 구간의 겹침 분(가로지름 분)
     * @param waivedBreakMinutes   쉬는 구간에 편입된 휴게 분 합(접함으로 늘어난 분 + 겹침 분)
     * @param recordOnly           합친 구간이 신청과 같아(붙은 휴게 없음) 시각 불변·요청 기록만 하는 경우 {@code true}
     * @param breakTimeRegistered  그날 스케줄에 휴게 시각 구간이 1개 이상 있으면 {@code true}(G-2 안내용)
     */
    public record BreakMergeResult(int exemptStartMin, int exemptEndMin, int requestMinutes, int chargeMinutes,
                                   int overlapBreakMinutes, int waivedBreakMinutes,
                                   boolean recordOnly, boolean breakTimeRegistered) {
    }

    /**
     * 시간차 신청 구간 {@code [startMin, endMin)} 에 접하거나 겹치는 스케줄 휴게 시각 구간을 쉬는 구간에
     * 합친다(순수 함수, 요청서 §1-2 / plan BW-03).
     *
     * <p>규칙:
     * <ul>
     *   <li>접함 판정 = {@code brkEnd == startMin || brkStart == endMin}, 겹침 = 통상 구간 겹침. 1·2구간 휴게 모두 검사.
     *       합친 뒤 또 접하는 휴게가 있으면 계속 합친다(반복 병합).</li>
     *   <li>휴게 구간은 근무 구간 프레임(자정 넘김 +1440, G-1 wrap)으로 정렬·클램프한 값을 쓴다. 신청 구간은
     *       그 구간을 포함하는 근무 구간의 프레임으로 맞춘 뒤 비교하고, 결과는 입력 프레임으로 되돌린다.</li>
     *   <li>휴게가 근무 구간 안에 클램프되어 있으므로 합친 구간은 근무 밖으로 나가지 않는다.</li>
     *   <li>차감 분 = {@code (endMin − startMin) − 겹침} — "차감 분은 불변"(신청한 근로 시간만 차감).
     *       예 A: 14~18 + 휴게 13~14(접함) → 저장 13:00~18:00, 차감 240. 예 B: 12~14 + 휴게 12~13(가로지름) →
     *       저장 12:00~14:00(동일, recordOnly), 차감 60.</li>
     * </ul>
     *
     * @param sch      그날 스케줄. {@code null} 이면 {@code null}.
     * @param startMin 신청 시작(분, 호출부 프레임 — 통상 {@code hhmmToMinutes} 원시 분)
     * @param endMin   신청 종료(분). {@code endMin <= startMin} 이면 {@code null}.
     * @return 편입 결과. 산출 불가(스케줄 없음/구간 비정상/신청 비정상)면 {@code null}.
     */
    public static BreakMergeResult mergeAdjacentBreaks(DailyScheduleVO sch, int startMin, int endMin) {
        if (sch == null || endMin <= startMin) {
            return null;
        }
        List<int[]> segments = buildSegments(sch);
        if (segments.isEmpty()) {
            return null;
        }
        List<int[]> breaks = breakIntervals(sch, segments);
        boolean breakTimeRegistered = !breaks.isEmpty();
        int requestMinutes = endMin - startMin;

        // 신청 구간을 근무 구간 프레임으로 정렬: [s+k, e+k] 를 완전히 포함하는 구간이 있는 offset k(0 또는 +1440)를
        //   찾는다(야간 2구간은 buildSegments 가 2구간을 +1440 으로 민다). 포함 구간이 없으면 원시 프레임 그대로
        //   비교한다(근무 밖 신청은 호출부 withinScheduledWorkHours 가 거부).
        int offset = 0;
        boolean aligned = false;
        for (int k = 0; k <= MINUTES_PER_DAY && !aligned; k += MINUTES_PER_DAY) {
            for (int[] seg : segments) {
                if (startMin + k >= seg[0] && endMin + k <= seg[1]) {
                    offset = k;
                    aligned = true;
                    break;
                }
            }
        }

        int s = startMin + offset;
        int e = endMin + offset;
        int overlap = 0;
        for (int[] b : breaks) {
            overlap += Math.max(0, Math.min(e, b[1]) - Math.max(s, b[0]));
        }

        // 반복 병합 — 접하거나 겹치는 휴게를 흡수. 휴게는 최대 2개라 2회면 수렴하나 일반형으로 둔다.
        int ms = s;
        int me = e;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int[] b : breaks) {
                if (b[1] >= ms && b[0] <= me) {
                    int ns = Math.min(ms, b[0]);
                    int ne = Math.max(me, b[1]);
                    if (ns != ms || ne != me) {
                        ms = ns;
                        me = ne;
                        changed = true;
                    }
                }
            }
        }

        int chargeMinutes = requestMinutes - overlap;
        // 편입 휴게 분 = 합친 길이 − 순수 근로(차감) 분.
        int waivedBreakMinutes = (me - ms) - chargeMinutes;
        boolean recordOnly = (ms == s && me == e);
        return new BreakMergeResult(ms - offset, me - offset, requestMinutes, chargeMinutes,
                overlap, waivedBreakMinutes, recordOnly, breakTimeRegistered);
    }

    /**
     * 분 → HHMM 표기(<b>화면 표시 전용</b>). 1440(자정 경계)은 기존 스케줄 종료 표기 규약과 대칭으로
     * {@code "2400"} 으로 내보낸다. 그 외는 {@code min % 1440} 을 4자리 0패딩한다.
     */
    public static String toHhmm(int min) {
        if (min == MINUTES_PER_DAY) {
            return "2400";
        }
        return hhmmOfDay(min);
    }

    /**
     * 분 → HHMM 표기(<b>저장 전용</b>). 항상 {@code min % 1440} 을 4자리 0패딩한다(1440 → {@code "0000"}).
     *
     * <p>★ Q5 정정(2026-08-07): 자정을 넘긴 값도 <b>날짜 컬럼은 근무일로 고정</b>한다. 자정 넘김은
     * "{@code END_TIME < START_TIME} 이면 종료가 익일"이라는 <b>시각 wrap</b> 으로만 표현하며,
     * 실제 instant 가 필요한 소비처는 <b>단일 진입점</b>
     * {@code PartialLeaveWindowUtils.exemptStampRange}(그날 원 스케줄을 프레임으로 정렬)로만 환산한다.
     * ★ 3차 재작업(§15-2, 2026-08-07): 양 끝이 모두 자정 이후인 야간 반차({@code '0115'~'0430'})는
     * 행 안에 wrap 신호가 없어 스케줄 없이는 환산할 수 없다 — 스케줄 없는 환산 경로는 남기지 않는다.
     *
     * <p>저장에 {@code "2400"} 을 쓰지 않는 이유: {@code DateTimeUtils.hhmmToMinutes}/{@code toMinuteStamp}
     * 등 시각 소비 경로가 hh&gt;23 을 거부(null)하므로, 연차 시각 컬럼에는 "2400" 을 넣을 수 없다.
     */
    public static String hhmmOfDay(int min) {
        int m = ((min % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY;
        return String.format("%02d%02d", m / 60, m % 60);
    }

    /** 휴게(분) 문자열 파싱. null/비정상/음수면 0. */
    private static int parseBrkMin(String brkMin) {
        if (brkMin == null || brkMin.isBlank()) {
            return 0;
        }
        try {
            int v = Integer.parseInt(brkMin.trim());
            return v > 0 ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
