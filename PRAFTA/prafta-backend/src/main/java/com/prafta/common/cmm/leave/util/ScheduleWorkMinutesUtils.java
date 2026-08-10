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
    //   정책서 신설 예정 조문 attd/08-leave.md §8.5.10.
    //   D = Σ(구간 종료 − 시작 − 휴게), H = D / 2 (정수 절사 — 차감 LEAVE_MINUTES 와 동일 연산자),
    //   경계 = 근무 시작부터 근로만 누적해 H 에 도달하는 시각(휴게는 건너뛰며 누적).
    // ================================================================

    /**
     * 반차 경계 산출 결과. 모든 분 값은 <b>근무일 00:00 = 0 기준</b>이며, 자정을 넘기는 야간 근무는
     * 1440 을 넘는 값으로 표현된다(예: 익일 01:15 = 1515). 저장용 HHMM 은 {@link #hhmmOfDay(int)} 로 얻는다
     * (★ Q5 정정: 저장 일자는 항상 근무일 — 자정 넘김은 {@code END_TIME < START_TIME} 시각 wrap 으로 표현).
     *
     * @param boundaryMin    경계 시각(분)
     * @param workStartMin   근무 시작 시각(분, 1구간 시작)
     * @param workEndMin     근무 종료 시각(분, 마지막 구간 종료)
     * @param exemptMinutes  반차 면제 근로분 H( = D / 2 정수 절사, 차감 LEAVE_MINUTES 와 동일 값)
     */
    public record HalfDayBoundary(int boundaryMin, int workStartMin, int workEndMin, int exemptMinutes) {
    }

    /**
     * 그날 스케줄의 반차 경계를 산출한다(순수 함수).
     *
     * <p>산출 규칙(요청서 §3-2 / plan HB-01):
     * <ol>
     *   <li>{@code D = dailyStdWorkMinutes(sch)}. {@code null} 이거나 2 미만이면 {@code null}(호출부 거부).</li>
     *   <li>{@code H = D / 2} — <b>정수 절사</b>. 반올림·5분 스냅 금지(차감 {@code leaveMinutes = daily/2} 와 동일해야 함).</li>
     *   <li>1구간 → (2구간) 순으로 근로를 이어붙여 누적하며 H 에 도달한 시각을 경계로 한다.</li>
     *   <li>휴게 <b>시각</b>이 모두 있으면 그 구간을 건너뛰며 누적한다(근무 구간과 교집합 클램프 —
     *       근무 밖에 저장된 휴게 시각 방어).</li>
     *   <li>휴게 시각이 없고 <b>분(brkMin)만</b> 있으면 건너뛸 구간이 없다 — 구간 앞에서부터 연속 누적하되
     *       그 구간의 기여를 {@code span - brkMin} 으로 제한한다(운영 최다 패턴, TC-05).</li>
     * </ol>
     *
     * @return 경계 산출 결과. 스케줄 없음/시각 비정상/D&lt;2 면 {@code null}.
     */
    public static HalfDayBoundary halfDayBoundary(DailyScheduleVO sch) {
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

        int walkTotal = 0;
        for (int[] part : workParts) {
            walkTotal += part[1] - part[0];
        }
        // 방어: 휴게 시각 폭이 brkMin 보다 크면 누적 가능분이 D 보다 작을 수 있다(데이터 불일치).
        //   실무상 H(=D/2) 를 못 채우는 경우는 없으나, 못 채우면 마지막 근로 종료로 클램프한다
        //   (null 을 반환해 기존에 되던 반차 신청이 막히는 회귀를 만들지 않는다).
        int target = Math.min(h, walkTotal);

        int acc = 0;
        int boundary = workEnd;
        for (int[] part : workParts) {
            int len = part[1] - part[0];
            if (acc + len >= target) {
                boundary = part[0] + (target - acc);
                break;
            }
            acc += len;
        }
        return new HalfDayBoundary(boundary, workStart, workEnd, h);
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

        Integer rbs = DateTimeUtils.hhmmToMinutes(brkStr);
        Integer rbe = DateTimeUtils.hhmmToMinutes(brkEnd);
        if (rbs != null && rbe != null && rbe > rbs) {
            // 휴게 시각을 구간과 같은 날짜 프레임으로 이동(야간 구간 지원).
            int bs = rbs;
            int be = rbe;
            while (bs < segStart) {
                bs += MINUTES_PER_DAY;
                be += MINUTES_PER_DAY;
            }
            // 근무 구간과 교집합 클램프 — 근무 밖에 저장된 휴게(F-2 갭2)는 무시된다.
            int cbs = Math.max(bs, segStart);
            int cbe = Math.min(be, segEnd);
            if (cbe > cbs) {
                addIfPositive(parts, segStart, cbs);
                addIfPositive(parts, cbe, segEnd);
                return;
            }
        }

        // 휴게 시각 미설정(운영 최다 패턴) — 건너뛸 구간 없이 연속 누적하되 기여를 span-brkMin 으로 제한.
        int segWork = span - brk;
        if (segWork > 0) {
            addIfPositive(parts, segStart, segStart + segWork);
        }
    }

    private static void addIfPositive(List<int[]> parts, int start, int end) {
        if (end > start) {
            parts.add(new int[] { start, end });
        }
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
