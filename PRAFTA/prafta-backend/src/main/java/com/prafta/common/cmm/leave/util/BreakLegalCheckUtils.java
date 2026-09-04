package com.prafta.common.cmm.leave.util;

import java.util.List;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.util.DateTimeUtils;

/**
 * BW-06(2026-09-04): 부분휴가(반차·시간차) 후 남는 근로에 대한 <b>법정 휴게 하한 경고</b> 산출(순수 함수, 요청서 §1-3).
 *
 * <pre>
 * R    = 그날 잔여 근로분(면제·차감 제외. 체크 시 휴게 포함 근로 = 근무 구간 − 쉬는 구간)
 * rest = 근무 구간 안에 남는 스케줄 휴게분(체크 시 0)
 * req  = R ≥ 480 ? 60 : R ≥ 240 ? 30 : 0        (근기법 제54조①)
 * OK   = rest ≥ req  ||  (체크 && R == 240)
 * </pre>
 *
 * <ul>
 *   <li>G-5: 근무타입이 <b>종일 기준으로도</b> 법정 휴게에 미달하면(운영 28개 휴게 미등록 타입) 배지 대상 제외
 *       ({@code eligible=false, ok=true}) — 부분휴가 때문에 생긴 부족만 표시한다.</li>
 *   <li>휴게 <b>분만</b> 등록된 타입(시각 없음)의 미체크 경계는 휴게 위치를 알 수 없어 {@code rest = 휴게분 합}으로
 *       보고(fail-open — 경고 배지는 안내용이라 불확실하면 띄우지 않는다), 체크는 G-2 로 시각 이동이 없으므로 미체크와 같다.</li>
 *   <li>문구는 서버가 만든다(앱/웹 재계산 금지 — 카운트다운 교훈). 형식: "이 날 근로 {H}시간 {M}분에 법정 휴게 {req}분이 없습니다".</li>
 *   <li>구간 프레임은 {@link ScheduleWorkMinutesUtils} 와 동일(근무일 00:00 = 0, 야간 1440 초과). 저장 시각(HHMM, wrap)은
 *       {@link #evaluateStored} 가 근무 구간 프레임으로 정렬한다.</li>
 * </ul>
 *
 * <p>BW-12(2026-09-04, 요청서 §7-1 / 정책서 attd/08-leave.md §8.5.10(e)): 부분휴가가 없는 통상 근무일도
 * 판정 대상에 넣는다 — <b>소정 240분·휴게 0</b> 인 단시간 근로자에게는 그 자체로 법정 휴게 30분이 없으므로,
 * "휴게 미이용 <b>상시</b> 요청"({@code TB_USER.BRK_WAIVE_STANDING_YN}) 여부를 판정 입력으로 받아
 * {@link #evaluateDay} 가 배지를 산출한다. 소정 240 <b>초과</b> · 휴게 0 타입은 종전대로 제외(G-5).
 */
public final class BreakLegalCheckUtils {

    private static final int MINUTES_PER_DAY = 1440;

    /** BW-12: 근기법 제54조① 단서가 말하는 "근로시간이 4시간인 경우" — 상시 요청 판정 대상 소정근로분. */
    private static final int SHORT_TIME_WORK_MIN = 240;

    private BreakLegalCheckUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 산출 결과.
     *
     * @param eligible      배지 판정 대상 여부(스케줄 없음/종일 기준 미달(G-5)/면제 구간 없음이면 false → ok=true)
     * @param actualWorkMin R — 잔여 근로분
     * @param restLeftMin   rest — 남는 스케줄 휴게분
     * @param requiredMin   req — 법정 필요 휴게분
     * @param ok            경고 없음
     * @param message       경고 문구(ok 면 null)
     */
    public record Result(boolean eligible, int actualWorkMin, int restLeftMin, int requiredMin,
                         boolean ok, String message) {

        /** 판정 비대상(경고 없음). */
        public static Result notEligible() {
            return new Result(false, 0, 0, 0, true, null);
        }

        /** 'Y'/'N' 표기. */
        public String warnYn() {
            return ok ? "N" : "Y";
        }
    }

    /** 근로분에 대한 법정 휴게 필요분(근기법 제54조①). */
    public static int requiredBreakMinutes(int workMin) {
        if (workMin >= 480) {
            return 60;
        }
        if (workMin >= 240) {
            return 30;
        }
        return 0;
    }

    /**
     * 저장 행(반차·시간차) 기준 산출 — {@code START_TIME/END_TIME}(HHMM, 종료 ≤ 시작이면 익일 wrap) + BRK_WAIVE_YN.
     * 시각이 없거나 파싱 불가면 판정 비대상.
     */
    public static Result evaluateStored(DailyScheduleVO sch, String startHhmm, String endHhmm, boolean waive) {
        Integer s = DateTimeUtils.hhmmToMinutes(startHhmm);
        Integer e = DateTimeUtils.hhmmToMinutes(endHhmm);
        if (s == null || e == null) {
            return Result.notEligible();
        }
        int es = s;
        int ee = e;
        if (ee <= es) {
            ee += MINUTES_PER_DAY; // 저장 규약: 종료 ≤ 시작 = 익일
        }
        return evaluate(sch, es, ee, waive);
    }

    /**
     * 면제(쉬는) 구간 {@code [exemptStartMin, exemptEndMin)} 기준 산출.
     * 입력 프레임이 근무 구간 프레임과 다르면(원시 0..1439 등) 겹침이 최대가 되는 오프셋(0/±1440)으로 정렬한다.
     *
     * @param sch            그날 스케줄(null 이면 비대상)
     * @param exemptStartMin 쉬는 구간 시작(분)
     * @param exemptEndMin   쉬는 구간 종료(분, 시작 초과)
     * @param waive          휴게 무시 체크 여부
     */
    public static Result evaluate(DailyScheduleVO sch, int exemptStartMin, int exemptEndMin, boolean waive) {
        if (sch == null || exemptEndMin <= exemptStartMin) {
            return Result.notEligible();
        }
        List<int[]> segs = ScheduleWorkMinutesUtils.workSegments(sch);
        if (segs.isEmpty()) {
            return Result.notEligible();
        }
        Integer daily = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(sch);
        if (daily == null) {
            return Result.notEligible();
        }
        List<int[]> breaks = ScheduleWorkMinutesUtils.breakTimeIntervals(sch);
        boolean breakTimeRegistered = !breaks.isEmpty();
        int dayRest = breakTimeRegistered ? totalLength(breaks) : ScheduleWorkMinutesUtils.totalBreakMinutes(sch);

        // G-5: 종일 기준으로도 법정 휴게 미달인 타입은 배지 대상 제외(부분휴가로 생긴 부족만 표시).
        if (dayRest < requiredBreakMinutes(daily)) {
            return Result.notEligible();
        }

        int[] exempt = alignToFrame(segs, exemptStartMin, exemptEndMin);
        if (exempt == null) {
            return Result.notEligible(); // 근무 구간과 전혀 겹치지 않는 면제 구간 — 판정 불가
        }

        int r;
        int rest;
        if (waive && breakTimeRegistered) {
            // 체크: 휴게를 건너뛰지 않은 근무 구간에서 쉬는 구간을 뺀 나머지가 전부 근로(휴게 포함 근로), 남는 휴게 0.
            r = minutesOutside(segs, exempt);
            rest = 0;
        } else {
            // 미체크(또는 분만 타입의 체크 = G-2 시각 불변): 근로 조각(휴게 제외)에서 쉬는 구간을 뺀 나머지.
            List<int[]> parts = ScheduleWorkMinutesUtils.workParts(sch);
            r = minutesOutside(parts, exempt);
            rest = breakTimeRegistered
                    ? minutesOutside(breaks, exempt)
                    : ScheduleWorkMinutesUtils.totalBreakMinutes(sch); // 위치 미상 — 남아 있다고 본다(fail-open)
        }
        int req = requiredBreakMinutes(r);
        boolean ok = rest >= req || (waive && breakTimeRegistered && r == 240);
        String message = ok ? null : buildMessage(r, req, rest);
        return new Result(true, r, rest, req, ok, message);
    }

    /**
     * BW-12(§7-1): <b>부분휴가가 없는 통상 근무일</b>의 법정 휴게 하한 판정 — 단시간(소정 240분·휴게 0) 근로자 전용.
     *
     * <p>대상: 그날 배정 스케줄의 소정근로가 <b>정확히 240분</b>이고 스케줄 휴게가 <b>0</b> 인 날.
     * 이 경우 근로 4시간에 법정 휴게 30분이 아예 없으므로, 근로자 본인이 마이페이지에서 켠
     * "휴게 미이용 상시 요청"이 있으면 적법(경고 없음), 없으면 경고 배지를 띄운다(차단 없음).
     *
     * <p>제외:
     * <ul>
     *   <li>소정 240 초과(예 09~18 · 휴게 0)는 종전대로 배지 대상 아님 — G-5(운영 휴게 미등록 타입 소음 차단).</li>
     *   <li>휴게가 등록된 타입은 본 항 대상 아님(부분휴가 축 = {@link #evaluate} 가 담당).</li>
     *   <li>그날 부분휴가(반차·시간차)가 있으면 대상 아님 — 잔여 근로가 240 미만이라 법정 휴게 요구가 없고,
     *       휴게 미이용 <b>건별</b> 요청은 사용 행 기준 {@link #evaluateStored} 가 이미 판정한다.</li>
     * </ul>
     *
     * @param sch             그날 배정 스케줄(null 이면 비대상)
     * @param standing        휴게 미이용 상시 요청 여부(TB_USER.BRK_WAIVE_STANDING_YN = 'Y')
     * @param hasPartialLeave 그날 확정 부분휴가(연차 사용) 존재 여부
     */
    public static Result evaluateDay(DailyScheduleVO sch, boolean standing, boolean hasPartialLeave) {
        if (sch == null || hasPartialLeave) {
            return Result.notEligible();
        }
        Integer daily = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(sch);
        if (daily == null || daily != SHORT_TIME_WORK_MIN) {
            return Result.notEligible();
        }
        List<int[]> breaks = ScheduleWorkMinutesUtils.breakTimeIntervals(sch);
        int dayRest = breaks.isEmpty() ? ScheduleWorkMinutesUtils.totalBreakMinutes(sch) : totalLength(breaks);
        if (dayRest > 0) {
            return Result.notEligible();
        }
        int req = requiredBreakMinutes(daily);
        if (standing) {
            return new Result(true, daily, 0, req, true, null);
        }
        return new Result(true, daily, 0, req, false, buildMessage(daily, req, 0));
    }

    /** 경고 문구(서버 생성). rest 가 0 이면 "없습니다", 일부 남아 있으면 "부족합니다(남은 휴게 N분)". */
    static String buildMessage(int workMin, int requiredMin, int restMin) {
        int h = workMin / 60;
        int m = workMin % 60;
        StringBuilder sb = new StringBuilder("이 날 근로 ").append(h).append("시간");
        if (m > 0) {
            sb.append(' ').append(m).append('분');
        }
        sb.append("에 법정 휴게 ").append(requiredMin).append("분이 ");
        if (restMin <= 0) {
            sb.append("없습니다");
        } else {
            sb.append("부족합니다(남은 휴게 ").append(restMin).append("분)");
        }
        return sb.toString();
    }

    /** 면제 구간을 근무 구간 프레임으로 정렬(0/+1440/−1440 중 겹침 최대). 겹침 0 이면 null. */
    private static int[] alignToFrame(List<int[]> segs, int es, int ee) {
        int[] best = null;
        int bestOverlap = 0;
        for (int offset : new int[] { 0, MINUTES_PER_DAY, -MINUTES_PER_DAY }) {
            int s = es + offset;
            int e = ee + offset;
            int overlap = 0;
            for (int[] seg : segs) {
                overlap += Math.max(0, Math.min(e, seg[1]) - Math.max(s, seg[0]));
            }
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = new int[] { s, e };
            }
        }
        return best;
    }

    /** 구간 목록에서 {@code exempt} 와 겹치지 않는 분의 합. */
    private static int minutesOutside(List<int[]> ranges, int[] exempt) {
        int total = 0;
        for (int[] r : ranges) {
            int len = r[1] - r[0];
            int overlap = Math.max(0, Math.min(r[1], exempt[1]) - Math.max(r[0], exempt[0]));
            total += Math.max(0, len - overlap);
        }
        return total;
    }

    private static int totalLength(List<int[]> ranges) {
        int total = 0;
        for (int[] r : ranges) {
            total += r[1] - r[0];
        }
        return total;
    }
}
