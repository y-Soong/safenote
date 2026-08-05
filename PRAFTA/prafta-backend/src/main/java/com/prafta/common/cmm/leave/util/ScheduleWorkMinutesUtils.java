package com.prafta.common.cmm.leave.util;

import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.util.DateTimeUtils;

/**
 * 근무 스케줄 소정근로분 계산 순수 함수 (개인 분모 개편 PC-03 — 단일 출처).
 *
 * <p>기존 {@code LeaveDeductionServiceImpl}의 private 구간 계산(segmentWorkMinutes)을 공용 추출했다.
 * <ul>
 *   <li>그날 소정근로분 D(마일스톤 하한 기준 — {@code getDailyStdWorkMinutes})와</li>
 *   <li>실차감 분모 conv(당일 배정 스케줄 소정근로분 — {@code resolveDailyConvMinutes}, E1)와</li>
 *   <li>참고 표시 분모(기본 근무타입 소정근로분 — {@code resolvePersonalConvMinutes}, E4 전용)가</li>
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
