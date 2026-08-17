package com.prafta.common.cmm.leave.util;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.util.DateTimeUtils;

/**
 * 부분연차(반차 SYS025 '01') 시각을 반영한 "유효 소정 구간" 산출 순수 함수
 * (반차 시간대 도입 HB-05/07/08, 2026-08-07).
 *
 * <p>정책서 신설 예정 조문 {@code attd/10-attendance-calc.md} §10.1.1.
 * 반차가 확정된 날은 스케줄 시각이 아니라 <b>반차 면제 구간을 제외한 구간</b>을 기준으로
 * 지각·조퇴를 판정하고, 초과근무 등록 가능 범위에서도 면제 구간을 뺀다.
 *
 * <p>파트(시작기준/종료기준)는 별도 컬럼 없이 <b>저장된 시각에서 역산</b>한다:
 * 면제 구간이 구간 앞쪽을 덮으면 시작기준(늦게 출근), 뒤쪽을 덮으면 종료기준(일찍 퇴근)이다.
 *
 * <p>시각이 없는 구 반차 데이터(START_TIME NULL)는 본 유틸 대상이 아니며 호출부가 진입 전에
 * 걸러낸다(시각 부재 = 면제 구간 특정 불가).
 *
 * <p>★2026-08-17 사용자 확정(HB-05 §2 비목표 해제): <b>시간차(02·03·04)도 판정 면제 입력에
 * 포함한다</b> — 소정 경계에 붙은 시간차(예: 16:30~17:00 사용 후 16:32 퇴근)가 조퇴로
 * 오판정되던 문제. {@link #resolve} 는 기하학적으로 동작하므로 경계 접촉 시간차는 유효 소정을
 * 치환하고, 구간 가운데만 덮는 시간차는 원값을 유지한다(지각·조퇴 판정 불변 — 의도된 동작).
 * 소비처(웹 Attd_08/11·대시보드·하도급 스냅샷·앱 관리자/사용자)의 로딩 필터를 함께 확장했다.
 */
public final class PartialLeaveWindowUtils {

    /** 하루(분). 야간 구간(종료 ≤ 시작) 보정에 사용. */
    private static final int MINUTES_PER_DAY = 1440;

    /**
     * 연차 면제 구간 stamp 축의 상한(= 근무일+1 23:59).
     * {@code DateTimeUtils.toStampRange} / {@code AttdScheduleUtils} 와 동일한 48시간 윈도우.
     */
    private static final int MAX_STAMP = 4320;

    private PartialLeaveWindowUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 반차를 반영한 한 구간(WORK_SEQ)의 유효 소정 시각.
     *
     * @param planStart   판정용 시작 시각(HHMM). {@code fullyExempt} 면 {@code null}
     * @param planEnd     판정용 종료 시각(HHMM). {@code fullyExempt} 면 {@code null}
     * @param fullyExempt 그 구간 전체가 반차 면제 구간에 덮여 판정 대상이 아닌지 여부
     */
    public record EffectiveWorkWindow(String planStart, String planEnd, boolean fullyExempt) {
    }

    /**
     * 그날 확정 부분연차 1건의 면제 구간(HHMM 쌍).
     *
     * <p>모듈별 결과 record(예: {@code AttdListsResult}, {@code LeaveExemptWindowResult})를 이 타입으로
     * 옮겨 담아 {@link #resolveAll(String, String, List)} 에 넘긴다.
     */
    public record LeaveWindow(String startHhmm, String endHhmm) {
    }

    /**
     * 그날 면제 구간 <b>여러 건</b>을 순차 적용해 유효 소정 구간을 산출한다(D-3, 2026-08-07).
     *
     * <p>반차는 하루 2건이 성립한다 — 시작기준(0.5) + 종료기준(0.5) = 1.0 이라 하루 1.0 초과 가드
     * ({@code ATTD_400_111})를 통과하고, 두 구간이 경계에서 맞닿아(좌폐우개) 겹침 검사도 통과한다.
     * 이때 단건만 반영하면 <b>종일 쉰 사람에게 지각·조퇴가 뜬다</b>. 두 파트는 경계에서 이어지므로
     * {@link #resolve} 를 순차 적용하면 앞/뒤가 차례로 깎여 최종적으로 {@code fullyExempt} 가 된다
     * (적용 순서와 무관).
     *
     * @return 유효 소정 구간. 입력이 비었으면 원값 그대로.
     */
    public static EffectiveWorkWindow resolveAll(String schStrHhmm, String schEndHhmm,
                                                 List<LeaveWindow> leaves) {
        EffectiveWorkWindow cur = new EffectiveWorkWindow(schStrHhmm, schEndHhmm, false);
        if (leaves == null || leaves.isEmpty()) {
            return cur;
        }
        for (LeaveWindow lv : leaves) {
            if (lv == null || lv.startHhmm() == null || lv.endHhmm() == null) {
                continue;
            }
            cur = resolve(cur.planStart(), cur.planEnd(), lv.startHhmm(), lv.endHhmm());
            if (cur.fullyExempt()) {
                return cur;
            }
        }
        return cur;
    }

    /**
     * 판정용 시각이 <b>근무일 당일인지 익일인지</b>를 원 스케줄 프레임으로 판정한다(D-1, 2026-08-07).
     *
     * <p>야간 스케줄(종료 &lt; 시작, 예: 22:00~04:30)에서는 스케줄 시작보다 이른 시각이 전부 익일이다.
     * 반차 경계가 자정을 넘기면 유효 소정 시각도 익일 값이 되는데(예: 시작기준 반차 → 판정용 시작 01:15),
     * 이를 근무일 당일로 두면 "01:15 출근"이 지각으로 오판정된다.
     * 종전 규칙(유효 종료 &lt; 유효 시작이면 종료만 익일)은 유효 시각끼리 비교해서 이 경우를 놓쳤다.
     *
     * <p>자정 종료 표기 {@code "2400"} 은 야간이 아니므로 offset 0 이며, 시각 환산(hh=24)에서 익일 00:00 이 된다.
     *
     * @param rawSchStart 원 스케줄 구간 시작(HHMM) — 반차 반영 <b>전</b> 값
     * @param rawSchEnd   원 스케줄 구간 종료(HHMM) — 반차 반영 <b>전</b> 값
     * @param hhmm        판정용 시각(HHMM)
     * @return 0(근무일 당일) 또는 1(익일). 입력이 비정상이면 0.
     */
    public static int dayOffsetOf(String rawSchStart, String rawSchEnd, String hhmm) {
        if (rawSchStart == null || rawSchEnd == null || hhmm == null
                || rawSchStart.length() != 4 || rawSchEnd.length() != 4 || hhmm.length() != 4) {
            return 0;
        }
        boolean night = rawSchEnd.compareTo(rawSchStart) < 0;
        if (!night) {
            return 0;
        }
        return (hhmm.compareTo(rawSchStart) < 0) ? 1 : 0;
    }

    /** 근태 판정 상태 — 출근기록 없음. */
    public static final String STATUS_ABSENT = "ABSENT";
    /** 근태 판정 상태 — 지각. */
    public static final String STATUS_LATE = "LATE";
    /** 근태 판정 상태 — 조퇴. */
    public static final String STATUS_EARLY_LEAVE = "EARLY_LEAVE";
    /** 근태 판정 상태 — 정상. */
    public static final String STATUS_NORMAL = "NORMAL";

    /**
     * 반차를 반영한 <b>차수 1건의 근태 상태</b> 판정 — 4차(NF-1/NF-2, 2026-08-07)의 단일 진입점.
     *
     * <p>{@code Attd08ServiceImpl.resolveStatus} 와 <b>동일 규칙</b>이다:
     * 출근기록 없으면 {@code ABSENT}(반차 유무 무관) → 유효 소정 시작보다 늦게 출근하면 {@code LATE}
     * → 유효 소정 종료보다 일찍 퇴근하면 {@code EARLY_LEAVE} → 그 외 {@code NORMAL}.
     *
     * <p>★ 판정용 시각의 <b>일자 프레임은 원 스케줄</b>({@code rawSchStart}/{@code rawSchEnd})로 잡는다
     * ({@link #dayOffsetOf}). 야간 스케줄에서 스케줄 시작보다 이른 시각은 익일이며, 문자열 비교로는
     * 이 구분이 불가능하다(2차 D-1 재발 지점). 자정 종료 표기 {@code "2400"} 은
     * {@link #judgeStamp} 가 hh=24 로 환산한다.
     *
     * <p>반차 미보유(또는 시각 없는 구 반차)면 {@code leaves} 를 비워 호출하면 되고, 그때는
     * 원 스케줄 그대로 판정한다(종전 동작 불변).
     *
     * @param workYmd     근무일(YYYYMMDD)
     * @param rawSchStart 그 차수의 <b>원</b> 스케줄 시작(HHMM) — 반차 반영 전
     * @param rawSchEnd   그 차수의 <b>원</b> 스케줄 종료(HHMM) — 반차 반영 전
     * @param leaves      그날 확정 부분연차 면제 구간(0~n건). null/빈값 허용
     * @return {@code ABSENT} | {@code LATE} | {@code EARLY_LEAVE} | {@code NORMAL}.
     *         근무일이 비정상이면 {@code null}(판정 불가 — 호출부가 종전 값을 유지한다)
     */
    public static String resolveAttdStatus(String workYmd, String rawSchStart, String rawSchEnd,
                                           List<LeaveWindow> leaves,
                                           String inDate, String inTime,
                                           String outDate, String outTime) {
        if (workYmd == null || workYmd.length() != 8) {
            return null;
        }
        String actInTime = blankToNull(inTime);
        if (actInTime == null) {
            // 출근 기록이 없으면 결근(반차 유무와 무관 — Attd_08 동일).
            return STATUS_ABSENT;
        }

        String rawStart = blankToNull(rawSchStart);
        String rawEnd = blankToNull(rawSchEnd);
        String planStart = rawStart;
        String planEnd = rawEnd;
        if (leaves != null && !leaves.isEmpty() && planStart != null && planEnd != null) {
            EffectiveWorkWindow eff = resolveAll(planStart, planEnd, leaves);
            planStart = eff.fullyExempt() ? null : blankToNull(eff.planStart());
            planEnd = eff.fullyExempt() ? null : blankToNull(eff.planEnd());
        }

        // 지각: 실제 출근 일시 > 유효 소정 시작 일시.
        if (planStart != null) {
            Long schStart = judgeStamp(judgeYmd(workYmd, rawStart, rawEnd, planStart), planStart);
            Long actIn = judgeStamp(blankToNull(inDate) != null ? inDate : workYmd, actInTime);
            if (schStart != null && actIn != null && actIn > schStart) {
                return STATUS_LATE;
            }
        }

        // 조퇴: 실제 퇴근 일시 < 유효 소정 종료 일시.
        String actOutTime = blankToNull(outTime);
        if (planEnd != null && actOutTime != null) {
            Long schEnd = judgeStamp(judgeYmd(workYmd, rawStart, rawEnd, planEnd), planEnd);
            Long actOut = judgeStamp(blankToNull(outDate) != null ? outDate : workYmd, actOutTime);
            if (schEnd != null && actOut != null && actOut < schEnd) {
                return STATUS_EARLY_LEAVE;
            }
        }

        return STATUS_NORMAL;
    }

    /**
     * 판정용 시각이 속한 일자(근무일 또는 익일) — 원 스케줄 프레임 기준({@link #dayOffsetOf}).
     * {@code Attd08ServiceImpl.shiftYmd} / {@code Attd11ServiceImpl.shiftYmd} 와 동일.
     */
    public static String judgeYmd(String workYmd, String rawSchStart, String rawSchEnd, String hhmm) {
        int offset = dayOffsetOf(rawSchStart, rawSchEnd, hhmm);
        return (offset == 0) ? workYmd : DateTimeUtils.plusDays(workYmd, offset);
    }

    /**
     * (일자 + HHMM) → 절대 분 stamp(1970-01-01 00:00 = 0). 파싱 불가면 {@code null}.
     *
     * <p>스케줄 종료 표기 {@code "2400"}(= 익일 00:00)을 hh=24 로 그대로 환산한다.
     * ({@code DateTimeUtils.toMinuteStamp} 는 hh&gt;23 을 거부해 자정 종료 스케줄의 조퇴 판정이
     * 통째로 스킵된다 — D-1 의 "2400 무발동"과 같은 함정이라 여기서는 쓰지 않는다.)
     */
    public static Long judgeStamp(String ymd, String hhmm) {
        if (ymd == null || ymd.length() != 8 || hhmm == null || hhmm.length() != 4) {
            return null;
        }
        java.time.LocalDate d = DateTimeUtils.parseYyyymmdd(ymd);
        if (d == null) {
            return null;
        }
        try {
            int hh = Integer.parseInt(hhmm.substring(0, 2));
            int mm = Integer.parseInt(hhmm.substring(2, 4));
            if (hh < 0 || hh > 24 || mm < 0 || mm > 59) {
                return null;
            }
            return d.toEpochDay() * 1440L + hh * 60L + mm;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * 그날 원 스케줄 <b>한 구간</b>(HHMM 쌍). 연차 시각을 절대 시각으로 환산할 때의 <b>날짜 프레임</b>이다.
     *
     * @see #exemptStampRange(String, String, List)
     */
    public record ScheduleSegment(String startHhmm, String endHhmm) {
    }

    /**
     * 1·2구간 시각 쌍을 {@link ScheduleSegment} 목록으로 만든다(빈 구간은 제외).
     * 각 모듈의 스케줄 결과 record(예: {@code AllowedWindowResult}, {@code ScheduleWindowResult},
     * {@code DailyScheduleVO})를 프레임 인자로 옮겨 담을 때 쓴다.
     */
    public static List<ScheduleSegment> scheduleSegments(String fstStrHhmm, String fstEndHhmm,
                                                         String secStrHhmm, String secEndHhmm) {
        List<ScheduleSegment> out = new ArrayList<>(2);
        if (hasText(fstStrHhmm) && hasText(fstEndHhmm)) {
            out.add(new ScheduleSegment(fstStrHhmm, fstEndHhmm));
        }
        if (hasText(secStrHhmm) && hasText(secEndHhmm)) {
            out.add(new ScheduleSegment(secStrHhmm, secEndHhmm));
        }
        return out;
    }

    /**
     * ★ 연차 시각({@code START_TIME}/{@code END_TIME})을 절대 시각(분 stamp)으로 바꾸는
     * <b>단일 진입점</b>(3차 재작업 §15-2, 2026-08-07).
     *
     * <p>축은 {@code DateTimeUtils.toMinuteStamp} 와 동일(근무일 전날 00:00 = 0, 근무일 00:00 = 1440)이라
     * 스케줄·실근태 구간과 그대로 차집합/겹침 판정할 수 있다.
     *
     * <p><b>왜 스케줄을 필수 인자로 받는가</b>(N-1 근본 원인): 연차 행은
     * {@code START_DATE = END_DATE = 근무일} 고정이고 자정 넘김은 시각 wrap 으로만 표현된다.
     * {@code '2200'~'0200'} 은 "종료 &lt; 시작"이라는 신호가 행 안에 있지만,
     * <b>양 끝이 모두 자정 이후({@code '0115'~'0430'})면 행 안에 신호가 없다</b> — 그 정보는 스케줄에만 있다.
     * 스케줄 없이 환산하면 면제 구간이 정확히 1440분 앞에 배치되어 OT 가 fail-open 된다.
     * 그래서 스케줄 없이 환산하는 오버로드는 <b>두지 않는다</b>.
     *
     * <p>정렬은 지각·조퇴 경로가 써 온 {@link #alignLeave}(당일/익일 후보 중 스케줄 구간과 겹침이 큰 쪽 채택)를
     * 그대로 재사용한다(단일 출처).
     *
     * @param leaveStartHhmm 연차 {@code START_TIME}(HHMM)
     * @param leaveEndHhmm   연차 {@code END_TIME}(HHMM)
     * @param schedule       그날 <b>원</b> 스케줄 구간(1·2구간). 비었으면 프레임 부재 → {@code null}
     * @return {@code [시작분, 종료분)}. 산출 불가(스케줄 프레임 부재·시각 비정상·0폭)면 {@code null}
     *         — 호출부는 <b>스킵하지 말고</b> {@link #fullDayBlockStampRange()} 로 보수 처리한다(§15-2-3).
     */
    public static int[] exemptStampRange(String leaveStartHhmm, String leaveEndHhmm,
                                         List<ScheduleSegment> schedule) {
        List<int[]> frame = normalizeFrame(schedule);
        if (frame.isEmpty()) {
            return null; // 스케줄 프레임 부재 → 환산 불가(fail-open 금지 — 호출부가 보수 처리)
        }
        Integer ls = DateTimeUtils.hhmmToMinutes(leaveStartHhmm);
        // 종료는 자정 표기("2400")를 인정하는 종료 전용 파서(resolve 와 동일 규칙).
        Integer le = DateTimeUtils.schEndToMinutes(leaveEndHhmm);
        if (ls == null || le == null) {
            return null;
        }
        int[] aligned = alignLeave(ls, le, frame);
        if (aligned == null) {
            return null;
        }
        // 축 이동: 근무일 00:00 = 0 → 근무일 전날 00:00 = 0.
        int start = aligned[0] + MINUTES_PER_DAY;
        int end = aligned[1] + MINUTES_PER_DAY;
        return (end > start) ? new int[] { start, end } : null;
    }

    /**
     * 면제 구간을 특정할 수 없을 때 쓰는 <b>보수 구간</b>(그날 전체 차단, §15-2-3 fail-open 금지).
     *
     * <p>종전에는 산출 실패 시 그 건을 건너뛰었는데(= 면제가 통째로 사라져 OT 과다 인정),
     * 판정 근거가 없으면 인정하지 않는 쪽이 수당 과다 지급보다 안전하다. 호출부는 WARN 을 남긴다.
     */
    public static int[] fullDayBlockStampRange() {
        return new int[] { 0, MAX_STAMP };
    }

    /**
     * 스케줄 구간을 "근무일 00:00 = 0" 축의 단조증가 프레임으로 정규화한다.
     *
     * <p>야간(종료 ≤ 시작) 구간은 종료에 +1440. 2구간 스케줄에서 뒤 구간이 앞 구간보다 이른 시각이면
     * (예: 22:00~02:00 + 03:00~07:00) 뒤 구간 전체를 하루 뒤로 밀어 실제 순서를 복원한다.
     */
    private static List<int[]> normalizeFrame(List<ScheduleSegment> schedule) {
        List<int[]> out = new ArrayList<>(2);
        if (schedule == null || schedule.isEmpty()) {
            return out;
        }
        int prevEnd = Integer.MIN_VALUE;
        int carry = 0; // 앞 구간보다 이른 뒤 구간에 누적 적용하는 일 단위 보정
        for (ScheduleSegment seg : schedule) {
            if (seg == null) {
                continue;
            }
            Integer s = DateTimeUtils.hhmmToMinutes(seg.startHhmm());
            // 종료는 자정 경계 근무("2400"=1440)를 인정하는 종료 전용 파서 사용.
            Integer e = DateTimeUtils.schEndToMinutes(seg.endHhmm());
            if (s == null || e == null) {
                continue;
            }
            int segStart = s + carry;
            if (prevEnd != Integer.MIN_VALUE && segStart < prevEnd) {
                carry += MINUTES_PER_DAY;
                segStart += MINUTES_PER_DAY;
            }
            int segEnd = e + carry;
            if (segEnd <= segStart) {
                segEnd += MINUTES_PER_DAY; // 야간 자정 넘김
            }
            if (segEnd <= segStart) {
                continue; // 0폭(데이터 결함) — 프레임에서 제외
            }
            out.add(new int[] { segStart, segEnd });
            prevEnd = segEnd;
        }
        return out;
    }

    /**
     * 스케줄 구간 [{@code schStrHhmm}, {@code schEndHhmm}) 에서 반차 면제 구간
     * [{@code leaveStartHhmm}, {@code leaveEndHhmm}) 를 제외한 유효 소정 구간을 산출한다.
     *
     * <ul>
     *   <li>면제 구간이 구간 전체를 덮으면 {@code fullyExempt=true}(지각·조퇴 판정 제외).</li>
     *   <li>앞쪽만 덮으면 시작을 면제 종료로 치환(시작기준 반차 — 늦게 출근).</li>
     *   <li>뒤쪽만 덮으면 종료를 면제 시작으로 치환(종료기준 반차 — 일찍 퇴근).</li>
     *   <li>겹치지 않거나 가운데만 덮으면(시간차 형태) 원값 유지 — 본 작업은 반차만 다룬다.</li>
     * </ul>
     *
     * <p>야간(종료 ≤ 시작) 구간과 2구간 스케줄을 지원한다. 면제 구간은 당일/익일 두 표현 중
     * 구간과 더 많이 겹치는 쪽을 채택한다(2구간 근무에서 1구간 시각이 2구간으로 잘못 밀리는 것 방지).
     *
     * @return 유효 소정 구간. 입력이 비정상이면 원값 그대로({@code fullyExempt=false}).
     */
    public static EffectiveWorkWindow resolve(String schStrHhmm, String schEndHhmm,
                                              String leaveStartHhmm, String leaveEndHhmm) {
        EffectiveWorkWindow unchanged = new EffectiveWorkWindow(schStrHhmm, schEndHhmm, false);

        Integer s = DateTimeUtils.hhmmToMinutes(schStrHhmm);
        // 종료는 자정 경계 근무("2400"=1440) 를 인정하는 종료 전용 파서 사용.
        Integer e = DateTimeUtils.schEndToMinutes(schEndHhmm);
        Integer ls = DateTimeUtils.hhmmToMinutes(leaveStartHhmm);
        Integer le = DateTimeUtils.schEndToMinutes(leaveEndHhmm);
        if (s == null || e == null || ls == null || le == null) {
            return unchanged;
        }

        int segStart = s;
        int segEnd = (e <= segStart) ? e + MINUTES_PER_DAY : e;

        int[] leave = alignLeave(ls, le, segStart, segEnd);
        if (leave == null) {
            return unchanged;
        }
        int lvStart = leave[0];
        int lvEnd = leave[1];

        // 겹침 없음 → 원값 유지.
        if (lvEnd <= segStart || lvStart >= segEnd) {
            return unchanged;
        }
        // 구간 전체 면제 → 판정 제외.
        if (lvStart <= segStart && lvEnd >= segEnd) {
            return new EffectiveWorkWindow(null, null, true);
        }
        // 앞쪽 면제(시작기준) → 시작을 면제 종료로 치환.
        if (lvStart <= segStart) {
            return new EffectiveWorkWindow(ScheduleWorkMinutesUtils.hhmmOfDay(lvEnd), schEndHhmm, false);
        }
        // 뒤쪽 면제(종료기준) → 종료를 면제 시작으로 치환.
        if (lvEnd >= segEnd) {
            return new EffectiveWorkWindow(schStrHhmm, ScheduleWorkMinutesUtils.hhmmOfDay(lvStart), false);
        }
        // 가운데만 덮는 형태(시간차) — 반차 경계는 항상 구간의 앞/뒤 끝을 포함하므로 도달하지 않는다.
        return unchanged;
    }

    /**
     * 면제 구간을 구간과 같은 날짜 프레임으로 정렬한다.
     * 당일 표현과 익일(+1440) 표현 중 구간과 더 많이 겹치는 쪽을 채택한다.
     *
     * @return {@code [시작, 종료)} 분. 0폭이면 {@code null}.
     */
    private static int[] alignLeave(int ls, int le, int segStart, int segEnd) {
        return alignLeave(ls, le, List.of(new int[] { segStart, segEnd }));
    }

    /**
     * {@link #alignLeave(int, int, int, int)} 의 다구간 버전 — 겹침을 전 구간 합으로 비교한다.
     * 2구간 스케줄에서 1구간 시각이 2구간으로 잘못 밀리는 것을 방지한다.
     *
     * @param frame {@link #normalizeFrame} 이 만든 단조증가 구간 목록(근무일 00:00 = 0 축)
     */
    private static int[] alignLeave(int ls, int le, List<int[]> frame) {
        int endSameDay = (le <= ls) ? le + MINUTES_PER_DAY : le;
        int[] candidateA = new int[] { ls, endSameDay };
        int[] candidateB = new int[] { ls + MINUTES_PER_DAY, endSameDay + MINUTES_PER_DAY };
        if (candidateA[1] <= candidateA[0]) {
            return null;
        }
        int overlapA = 0;
        int overlapB = 0;
        for (int[] seg : frame) {
            overlapA += overlap(candidateA, seg[0], seg[1]);
            overlapB += overlap(candidateB, seg[0], seg[1]);
        }
        return (overlapB > overlapA) ? candidateB : candidateA;
    }

    /** 두 구간의 겹침 분(겹치지 않으면 0). */
    private static int overlap(int[] range, int segStart, int segEnd) {
        int lo = Math.max(range[0], segStart);
        int hi = Math.min(range[1], segEnd);
        return Math.max(0, hi - lo);
    }

    /** 분 stamp 구간 두 개가 겹치는지(좌폐우개 — 경계 일치는 겹침 아님). */
    public static boolean stampOverlaps(int[] a, int[] b) {
        if (a == null || b == null) {
            return false;
        }
        return a[0] < b[1] && b[0] < a[1];
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
