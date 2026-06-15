package com.prafta.web.attd.attd07.util;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.result.AllowedWindowResult;

/**
 * attd07 도메인 전용 헬퍼. {@link AllowedWindowResult}
 * (TB_SCH_MGMT의 raw HH:mm 컬럼 + 실근태 actual)를
 * workYmd 기준 분 stamp 구간으로 변환하여
 * {@code com.prafta.common.util.IntervalUtils}가 사용할 수 있게 만든다.
 *
 * <p>이 로직은 SCH_MGMT / USER_ATTD_MGMT 컬럼 형태에 강하게 결합되어 있어
 * 다른 모듈에는 유용하지 않으므로 attd07 패키지에 둔다.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class AttdScheduleUtils {

    private AttdScheduleUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 단일 스케줄 구간을 workYmd 기준 분 stamp 범위로 빌드한다.
     *
     * <p>TB_SCH_MGMT는 HH:mm만 저장하고 날짜는 저장하지 않으므로 다음 컨벤션을 따른다:
     * <ul>
     *   <li>start는 항상 workYmd에 anchor된다. [QA 재작업 D1] plan stamp origin을
     *       {@link DateTimeUtils#toMinuteStamp(String, String, String)}와 동일 기준
     *       (workYmd-1 00:00 = 0 → workYmd 00:00 = 1440)으로 통일하므로
     *       {@code sMin = 1440 + hhmm}이다.</li>
     *   <li>endHHmm &lt; startHHmm인 경우 자정을 넘는 스케줄로 보고
     *       end stamp에 +1440을 더한다 (workYmd+1에서 종료).</li>
     *   <li>endHHmm == startHHmm이면 0분 길이 스케줄로, 데이터 결함으로 간주하여
     *       null을 반환(skip)한다. +1440을 더해서 잘못된 24시간 구간이 되는 것을 방지한다.</li>
     *   <li>결과 stamp 범위는 0..4320 안에 머문다 (actual과 동일, workYmd+1 23:59까지).</li>
     * </ul>
     *
     * <p>{@code workYmd} 파라미터는 TB_SCH_MGMT 자체에 명시적인 날짜 컬럼이 없음에도
     * {@link #buildActualSegment(String, String, String, String, String)}와의
     * 시그니처 일관성을 위해 유지된다.
     */
    public static int[] buildPlanSegment(String workYmd, String startHHmm, String endHHmm) {
        if (workYmd == null || workYmd.length() != 8) return null;
        Integer s = DateTimeUtils.hhmmToMinutes(startHHmm);
        Integer e = DateTimeUtils.hhmmToMinutes(endHHmm);
        if (s == null || e == null) return null;
        // [QA 재작업 D1] plan stamp origin 통일: workYmd anchor 이므로 dayOffset=0,
        // toMinuteStamp 와 동일하게 기준점을 workYmd-1 00:00 으로 이동(+1440).
        int sMin = 1440 + s.intValue();
        int eMin = 1440 + e.intValue();
        // PRAFTA-003-1 - 두 케이스를 분리:
        //   end <  start  -> 자정을 넘는 스케줄 (+1440 shift)
        //   end == start  -> 0분 길이 스케줄 (데이터 결함, skip)
        if (eMin < sMin) {
            eMin += 1440;
        } else if (eMin == sMin) {
            return null;
        }
        // plan과 actual이 동일한 0..4320 경계를 공유하도록
        // DateTimeUtils.toStampRange와 같은 범위 가드를 적용한다.
        if (sMin < 0 || eMin > 4320 || eMin <= sMin) return null;
        return new int[] { sMin, eMin };
    }

    /**
     * SCH_MGMT의 스케줄 구간(plan1, plan2)을 workYmd 기준 분 stamp 리스트로 반환한다.
     * {@link #buildActualSegments(String, AllowedWindowResult)}와 동일한 origin을
     * 사용하므로 후속 merge / subtract 연산에서 동등한 비교가 가능하다.
     */
    public static List<int[]> buildScheduledSegments(String workYmd, AllowedWindowResult w) {
        List<int[]> out = new ArrayList<>(2);
        int[] s1 = buildPlanSegment(workYmd, w.plan1Start(), w.plan1End());
        if (s1 != null) out.add(s1);
        int[] s2 = buildPlanSegment(workYmd, w.plan2Start(), w.plan2End());
        if (s2 != null) out.add(s2);
        return out;
    }

    /**
     * 단일 actual 근무 구간을 workYmd 기준 분 stamp 범위로 빌드한다.
     * 양쪽 endpoint(date와 time, 각각 길이 8과 4)가 모두 존재해야 하며,
     * end는 start보다 엄격히 커야 하고, 결과 범위는 0..4320 안에 들어와야 한다.
     */
    public static int[] buildActualSegment(String workYmd,
                                           String inDate, String inTime,
                                           String outDate, String outTime) {
        if (inDate == null || inTime == null || outDate == null || outTime == null) {
            return null;
        }
        if (inDate.length() != 8 || outDate.length() != 8) {
            return null;
        }
        if (inTime.length() != 4 || outTime.length() != 4) {
            return null;
        }
        Integer s = DateTimeUtils.toMinuteStamp(workYmd, inDate, inTime);
        Integer e = DateTimeUtils.toMinuteStamp(workYmd, outDate, outTime);
        if (s == null || e == null) return null;
        int sMin = s.intValue();
        int eMin = e.intValue();
        if (eMin <= sMin) return null;
        // buildPlanSegment / DateTimeUtils.toStampRange와 동일한 경계.
        // [QA 재작업 D1] 상한 가드 2880 -> 4320 (오버나이트 OT 실근무가 workYmd+1로 넘어가는 케이스 지원).
        if (sMin < 0 || eMin > 4320) return null;
        return new int[] { sMin, eMin };
    }

    /**
     * raw 실제 출퇴근 구간(act1, act2)을 workYmd 기준 분 stamp 리스트로 반환한다.
     * 각 row는 (in/out) date/time 컬럼 쌍을 사용하므로 자정을 넘는 actual도 정확하게 stamp된다.
     *
     * <p>초과근무 등록 가능 범위는 "실근태 − 스케줄" 로 계산하므로 raw 시각을 사용한다.
     */
    public static List<int[]> buildActualSegments(String workYmd, AllowedWindowResult w) {
        List<int[]> out = new ArrayList<>(2);
        int[] s1 = buildActualSegment(workYmd, w.act1InDate(), w.act1InTime(),
                                      w.act1OutDate(), w.act1OutTime());
        if (s1 != null) out.add(s1);
        int[] s2 = buildActualSegment(workYmd, w.act2InDate(), w.act2InTime(),
                                      w.act2OutDate(), w.act2OutTime());
        if (s2 != null) out.add(s2);
        return out;
    }

    // ============================================================
    // PRAFTA-011 - WORK_SEQ 인덱스를 보존하는 구간 빌더
    //
    // 기존 buildScheduledSegments / buildActualSegments 는 1·2구간을
    // 단일 List 로 flatten 하므로 "1구간 actual ↔ 1구간 schedule" 같은
    // 구간별 매칭 계산이 불가능하다. 아래 메서드는 결과를 길이 3의 배열에
    // 담아 index 1 = WORK_SEQ 1, index 2 = WORK_SEQ 2 로 자리를 보존한다
    // (index 0 은 미사용). 해당 구간 데이터가 없으면 자리에 null 을 둔다.
    // ============================================================

    /**
     * SCH_MGMT 의 스케줄 구간(plan1 / plan2)을 WORK_SEQ 인덱스를 보존한
     * 길이 3의 배열로 반환한다.
     *
     * <ul>
     *   <li>index 1 - WORK_SEQ 1 스케줄 구간 (plan1). 없으면 null.</li>
     *   <li>index 2 - WORK_SEQ 2 스케줄 구간 (plan2). 없으면 null.</li>
     *   <li>index 0 - 미사용 (항상 null).</li>
     * </ul>
     *
     * 각 원소의 stamp 산출 규칙은 {@link #buildPlanSegment(String, String, String)}와 동일하다.
     */
    public static int[][] buildScheduledSegmentsBySeq(String workYmd, AllowedWindowResult w) {
        int[][] bySeq = new int[3][];
        bySeq[1] = buildPlanSegment(workYmd, w.plan1Start(), w.plan1End());
        bySeq[2] = buildPlanSegment(workYmd, w.plan2Start(), w.plan2End());
        return bySeq;
    }

    /**
     * raw 실제 출퇴근 구간(act1 / act2)을 WORK_SEQ 인덱스를 보존한 길이 3의 배열로 반환한다.
     *
     * <ul>
     *   <li>index 1 - WORK_SEQ 1 actual 구간 (act1). 없으면 null.</li>
     *   <li>index 2 - WORK_SEQ 2 actual 구간 (act2). 없으면 null.</li>
     *   <li>index 0 - 미사용 (항상 null).</li>
     * </ul>
     *
     * 각 원소의 stamp 산출 규칙은
     * {@link #buildActualSegment(String, String, String, String, String)}와 동일하며,
     * (in/out) date/time 컬럼 쌍을 사용하므로 자정을 넘는 actual 도 정확히 stamp 된다.
     *
     * <p>초과근무 등록 가능 범위는 "실근태 − 스케줄" 로 계산하므로 raw 출퇴근 시각을 사용한다.
     */
    public static int[][] buildActualSegmentsBySeq(String workYmd, AllowedWindowResult w) {
        int[][] bySeq = new int[3][];
        bySeq[1] = buildActualSegment(workYmd, w.act1InDate(), w.act1InTime(),
                                      w.act1OutDate(), w.act1OutTime());
        bySeq[2] = buildActualSegment(workYmd, w.act2InDate(), w.act2InTime(),
                                      w.act2OutDate(), w.act2OutTime());
        return bySeq;
    }
}
