package com.prafta.common.cmm.leave.util;

import java.util.List;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;

/**
 * 부분휴가 휴게 넘김 v2 — 법정 휴게 하한·넘길 수 있는 상한(cap) 산식 단일 출처 (BW2-02, 2026-09-05).
 *
 * <p>요청서 {@code 작업지시서_부분휴가-휴게넘김-v2-법정하한상한제.md} §1-1 산식을 그대로 옮긴 순수 함수 모음이다.
 * <pre>
 *   D    = 그날 소정근로분 ({@link ScheduleWorkMinutesUtils#dailyStdWorkMinutes})
 *   F    = 그날 고정연장분 ({@code FixedOtScheduleUtils.totalFixedOtMinutes} — DailyScheduleVO 밖에서 별도 조회, R5)
 *   X    = 부분휴가 면제 근로분 (반차 = D/2, 시간차 = 차감 분)
 *   R    = D − X + F                       // 그날 실근로(예정)
 *   B    = 회사 휴게분 (휴게 시각 폭 합, 시각 없으면 분 합)
 *   req  = R &lt; 240 ? 0 : R == 240 ? 0 : R &lt; 480 ? 30 : 60 + 30 * floor((R − 480) / 240)   // R6
 *   cap  = max(0, B − req)                 // 넘길 수 있는 최대(15분 단위 스테퍼 상한)
 * </pre>
 * R 은 W(넘길 분량)와 독립이다(휴게는 근로가 아니므로 넘겨도 R 불변) → 화면 진입 시 1회 계산.
 *
 * <p>★종전 v1 법정 체크 유틸의 {@code requiredBreakMinutes}(≥480→60, ≥240→30)와 <b>다르다</b>
 * (240 → 0: 개정 조문 "4시간 근로 시 요청으로 면제" — 시행일 게이트 없음, 8시간 초과분 4시간마다 +30).
 * 구 유틸은 BW2-07 에서 삭제되었고 본 클래스만 남는다.
 *
 * <p>정책서: 요청서 §1-1·R5·R6 / {@code attd/08-leave.md} §8.5.10(c)(BW2-12 개정) / {@code 02-regulations.md} §2.1 제54조.
 * 도메인 종속(연차/스케줄) 유틸이므로 common.util 이 아닌 모듈 내부에 둔다.
 */
public final class BreakWaiveCapUtils {

    /** 넘길 휴게 분량 W 의 선택 단위(분). 서버 검증 {@link #isValidStep} 과 앱 스테퍼 step 이 공용. */
    public static final int STEP_MIN = 15;

    /** 하루(분). 면제 구간 프레임 정렬(0/±1440)에 사용. */
    private static final int MINUTES_PER_DAY = 1440;

    /** 법정 휴게 하한 1단(4시간 근로) 경계. */
    private static final int LEGAL_TIER1_WORK_MIN = 240;

    /** 법정 휴게 하한 2단(8시간 근로) 경계. */
    private static final int LEGAL_TIER2_WORK_MIN = 480;

    private BreakWaiveCapUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 잔여 근로 R 에 대한 법정 휴게 하한(분) — 요청서 §1-1·R6.
     *
     * <ul>
     *   <li>R &lt; 240 → 0 (휴게 의무 없음)</li>
     *   <li>R == 240 → 0 (개정 조문: 4시간 근로자의 요청 시 면제 — 게이트 없음)</li>
     *   <li>240 &lt; R &lt; 480 → 30</li>
     *   <li>R ≥ 480 → 60 + 30 × floor((R − 480) / 240) (8시간 초과 4시간마다 +30)</li>
     * </ul>
     * 경계값: 239/240/241/479/480/719/720/960 → 0/0/30/30/60/60/90/120.
     *
     * @param remainWorkMin 잔여 근로 R(분). 음수는 0 으로 본다.
     */
    public static int requiredBreakMinutes(int remainWorkMin) {
        int r = Math.max(0, remainWorkMin);
        if (r <= LEGAL_TIER1_WORK_MIN) {
            return 0;
        }
        if (r < LEGAL_TIER2_WORK_MIN) {
            return 30;
        }
        return 60 + 30 * ((r - LEGAL_TIER2_WORK_MIN) / LEGAL_TIER1_WORK_MIN);
    }

    /**
     * 회사 휴게분 B — 휴게 <b>시각</b> 구간이 1개 이상 있으면 그 폭의 합(근무 구간 프레임·클램프 후),
     * 없으면(분만 등록/미등록) 1·2구간 휴게 분 합. 스케줄 null 이면 0.
     */
    public static int companyBreakMinutes(DailyScheduleVO sch) {
        if (sch == null) {
            return 0;
        }
        List<int[]> breakTimes = ScheduleWorkMinutesUtils.breakTimeIntervals(sch);
        if (!breakTimes.isEmpty()) {
            return totalLength(breakTimes);
        }
        return ScheduleWorkMinutesUtils.totalBreakMinutes(sch);
    }

    /**
     * 면제 구간 {@code [exemptStartMin, exemptEndMin)} 과 휴게 시각 구간의 겹침 분(편입 휴게분 산출용 — BW2-05).
     *
     * <p>면제 구간은 근무 구간 프레임으로 정렬한다(0/+1440/−1440 중 근무 구간과 겹침이 최대인 오프셋 —
     * 종전 v1 법정 체크 유틸의 {@code alignToFrame} 이관). 휴게 시각 미등록이거나 근무 구간과 겹치지 않으면 0.
     */
    public static int breakMinutesWithin(DailyScheduleVO sch, int exemptStartMin, int exemptEndMin) {
        if (sch == null || exemptEndMin <= exemptStartMin) {
            return 0;
        }
        List<int[]> segments = ScheduleWorkMinutesUtils.workSegments(sch);
        if (segments.isEmpty()) {
            return 0;
        }
        List<int[]> breakTimes = ScheduleWorkMinutesUtils.breakTimeIntervals(sch);
        if (breakTimes.isEmpty()) {
            return 0;
        }
        int[] exempt = alignToFrame(segments, exemptStartMin, exemptEndMin);
        if (exempt == null) {
            return 0;
        }
        int total = 0;
        for (int[] b : breakTimes) {
            total += Math.max(0, Math.min(b[1], exempt[1]) - Math.max(b[0], exempt[0]));
        }
        return total;
    }

    /** W 가 0 이상이고 {@link #STEP_MIN} 배수인지(요청 검증 ② — ATTD_400_221 판정 입력). */
    public static boolean isValidStep(int waiveMin) {
        return waiveMin >= 0 && waiveMin % STEP_MIN == 0;
    }

    /**
     * cap 산출 결과(요청서 §1-1 각 항의 값을 그대로 노출 — 안내 문구·로그·검증이 재계산 없이 사용).
     *
     * @param dailyStdMin          D — 그날 소정근로분
     * @param exemptMin            X — 면제 근로분(반차 = D/2, 시간차 = 차감 분)
     * @param fixedOtMin           F — 그날 고정연장분(전방+후방)
     * @param remainWorkMin        R = D − X + F (음수면 0 으로 클램프)
     * @param companyBreakMin      B — 회사 휴게분
     * @param requiredMin          req — 법정 휴게 하한
     * @param capMin               cap = max(0, B − req) — 넘길 수 있는 최대 분량
     * @param breakTimeRegistered  휴게 <b>시각</b> 구간이 1개 이상 있으면 {@code true}(false 면 분량 선택 불가 — G-2)
     */
    public record CapResult(int dailyStdMin, int exemptMin, int fixedOtMin, int remainWorkMin,
                            int companyBreakMin, int requiredMin, int capMin,
                            boolean breakTimeRegistered) {
    }

    /**
     * 그날 스케줄·고정연장·면제 분으로 cap 을 산출한다(순수 함수).
     *
     * @param sch        그날 스케줄. {@code null} 이거나 D 산출 불가면 {@code null}.
     * @param fixedOtMin F — 별도 조회한 고정연장분(없으면 0). 음수는 0 으로 본다.
     * @param exemptMin  X — 반차는 H(= D/2), 시간차는 차감 분. 음수는 0 으로 본다.
     */
    public static CapResult compute(DailyScheduleVO sch, int fixedOtMin, int exemptMin) {
        if (sch == null) {
            return null;
        }
        Integer d = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(sch);
        if (d == null) {
            return null;
        }
        int f = Math.max(0, fixedOtMin);
        int x = Math.max(0, exemptMin);
        int r = Math.max(0, d - x + f);
        int b = companyBreakMinutes(sch);
        int req = requiredBreakMinutes(r);
        int cap = Math.max(0, b - req);
        boolean breakTimeRegistered = !ScheduleWorkMinutesUtils.breakTimeIntervals(sch).isEmpty();
        return new CapResult(d, x, f, r, b, req, cap, breakTimeRegistered);
    }

    /**
     * 서버 생성 안내 문구(FE 재계산 금지 — day-schedule {@code brkWaiveReasonText}). 어미는 "~됩니다/~없습니다"(§7 Q9).
     *
     * <ul>
     *   <li>휴게 자체가 없음(B=0): "이 날은 등록된 휴게가 없어 넘길 휴게가 없습니다. 휴게 없이 근무 요청만 기록됩니다."</li>
     *   <li>휴게 시각 미등록(분만): "휴게 시각이 등록되지 않아 분량은 고를 수 없습니다. 휴게 없이 근무 요청만 기록됩니다."</li>
     *   <li>cap = 0: "이 날 남는 근로 …시간 → 법정 휴게 N분. 이 날은 법정 휴게 때문에 넘길 수 있는 휴게가 없습니다."</li>
     *   <li>cap &gt; 0: "이 날 남는 근로 …시간 → 법정 휴게 N분. 회사 휴게 B분 중 최대 cap분까지 넘길 수 있습니다."</li>
     * </ul>
     *
     * @return 안내 문구. {@code cap} 이 {@code null} 이면 {@code null}.
     */
    public static String guideText(CapResult cap) {
        if (cap == null) {
            return null;
        }
        if (cap.companyBreakMin() <= 0) {
            return "이 날은 등록된 휴게가 없어 넘길 휴게가 없습니다. 휴게 없이 근무 요청만 기록됩니다.";
        }
        if (!cap.breakTimeRegistered()) {
            return "휴게 시각이 등록되지 않아 분량은 고를 수 없습니다. 휴게 없이 근무 요청만 기록됩니다.";
        }
        StringBuilder sb = new StringBuilder(96);
        sb.append("이 날 남는 근로 ").append(formatHourMin(cap.remainWorkMin()))
          .append(" → 법정 휴게 ").append(cap.requiredMin()).append("분. ");
        if (cap.capMin() <= 0) {
            sb.append("이 날은 법정 휴게 때문에 넘길 수 있는 휴게가 없습니다.");
        } else {
            sb.append("회사 휴게 ").append(cap.companyBreakMin()).append("분 중 최대 ")
              .append(cap.capMin()).append("분까지 넘길 수 있습니다.");
        }
        return sb.toString();
    }

    /** 분 → "N시간 M분" / "N시간" / "M분" (0 이면 "0분"). */
    private static String formatHourMin(int minutes) {
        int m = Math.max(0, minutes);
        int h = m / 60;
        int mm = m % 60;
        if (h == 0) {
            return mm + "분";
        }
        if (mm == 0) {
            return h + "시간";
        }
        return h + "시간 " + mm + "분";
    }

    /** 면제 구간을 근무 구간 프레임으로 정렬(0/+1440/−1440 중 겹침 최대). 겹침 0 이면 null. */
    private static int[] alignToFrame(List<int[]> segments, int es, int ee) {
        int[] best = null;
        int bestOverlap = 0;
        for (int offset : new int[] { 0, MINUTES_PER_DAY, -MINUTES_PER_DAY }) {
            int s = es + offset;
            int e = ee + offset;
            int overlap = 0;
            for (int[] seg : segments) {
                overlap += Math.max(0, Math.min(e, seg[1]) - Math.max(s, seg[0]));
            }
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = new int[] { s, e };
            }
        }
        return best;
    }

    /** 구간 목록의 길이 합(분). */
    private static int totalLength(List<int[]> ranges) {
        int total = 0;
        for (int[] r : ranges) {
            total += r[1] - r[0];
        }
        return total;
    }
}
