package com.prafta.common.cmm.schedule.util;

import java.util.ArrayList;
import java.util.List;

import com.prafta.common.util.DateTimeUtils;

/**
 * PRAFTA-FIXEDOT-2: 근무타입 고정연장근무(전방·후방 FROM/TO 2쌍)의 구간 환산 단일 출처.
 *
 * <p>tb_sch_mgmt 의 PRE_FIXED_OT_STR/END_TIME(전방) · FIXED_OT_STR/END_TIME(후방) HHMM 쌍을
 * <b>일 anchor(해당 근무일 00:00 = 0분) 기준 분 구간 [start, end)</b> 목록으로 환산한다.
 * 소비처(웹/앱 OT 등록범위 · 교차일 겹침 가드 · 앱 홈 오버나이트 · 주52 잠정치)는 각자의
 * stamp 원점 오프셋(웹/앱 근태 stamp 프레임 +1440, 가드 delta*1440 등)만 더해 사용한다.
 *
 * <p>자정 넘김 규약(1단계 Attd01 검증 V2/V3/V5 와 동일 프레임):
 * <ul>
 *   <li>전방: 당일 내 구간만(V2 — 시작&lt;종료). wrap 형태는 데이터 결함으로 보고 skip.</li>
 *   <li>후방(소정 비-wrap 타입): 종료&lt;=시작이면 익일 걸침으로 +1440(V5, "2400"=1440 인정).
 *       종료==시작(0분/24시간 해석 모호)은 저장 검증이 거부하므로 방어적으로 skip.</li>
 *   <li>후방(소정 wrap 타입 — 1·2구간 중 하나라도 자정을 넘김): 소정 마지막 구간이 익일에
 *       끝나므로 후방 구간 전체가 <b>익일</b>이다(V3 — 일자 프레임 빈 구간 [마지막 소정 종료,
 *       1구간 시작) 안, 재차 wrap 불가). 시작·종료 모두 +1440.</li>
 * </ul>
 *
 * <p>★NULL 가드: 쌍이 NULL/빈값/형식 불량이면 해당 구간을 조용히 skip 한다 — 고정연장이 없는
 * 기존 근무타입 경로에서는 항상 빈 목록을 반환해 전 소비처가 현행과 완전히 동일하게 동작한다.
 */
public final class FixedOtScheduleUtils {

    private static final int DAY_MIN = 1440;

    private FixedOtScheduleUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 전방·후방 고정연장 구간을 일 anchor(당일 00:00 = 0) 분 구간 목록으로 환산한다.
     *
     * @param fstSchStrTime 소정 1구간 시작(HHMM) — 후방의 익일 배치 판정(소정 wrap 여부)에 사용
     * @param fstSchEndTime 소정 1구간 종료(HHMM)
     * @param secSchStrTime 소정 2구간 시작(HHMM, 없으면 NULL)
     * @param secSchEndTime 소정 2구간 종료(HHMM, 없으면 NULL)
     * @param preFixedOtStrTime 전방 고정연장 시작(HHMM, 없으면 NULL)
     * @param preFixedOtEndTime 전방 고정연장 종료(HHMM, 없으면 NULL)
     * @param fixedOtStrTime 후방 고정연장 시작(HHMM, 없으면 NULL)
     * @param fixedOtEndTime 후방 고정연장 종료(HHMM, 없으면 NULL)
     * @return [start, end) 분 구간 목록(전방 → 후방 순). 고정연장 미설정/환산 불가 구간은 제외.
     */
    public static List<int[]> fixedOtSegments(
            String fstSchStrTime, String fstSchEndTime,
            String secSchStrTime, String secSchEndTime,
            String preFixedOtStrTime, String preFixedOtEndTime,
            String fixedOtStrTime, String fixedOtEndTime) {

        List<int[]> out = new ArrayList<>(2);

        // 전방: 당일 내 구간만(V2). 시작>=종료(wrap 형태)는 skip.
        Integer preS = DateTimeUtils.hhmmToMinutes(preFixedOtStrTime);
        Integer preE = DateTimeUtils.schEndToMinutes(preFixedOtEndTime);
        if (preS != null && preE != null && preS < preE) {
            out.add(new int[] { preS, preE });
        }

        // 후방: 소정 wrap 여부에 따라 당일/익일 배치.
        Integer rearS = DateTimeUtils.hhmmToMinutes(fixedOtStrTime);
        Integer rearE = DateTimeUtils.schEndToMinutes(fixedOtEndTime);
        if (rearS != null && rearE != null && !rearS.equals(rearE)) {
            if (scheduleWraps(fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime)) {
                // 소정이 자정을 넘기는 타입: 후방은 전체가 익일(재차 wrap 불가 — V3/V5).
                // 방어: 종료<시작(저장 검증상 불가) 형태는 skip.
                if (rearS < rearE) {
                    out.add(new int[] { rearS + DAY_MIN, rearE + DAY_MIN });
                }
            } else {
                int e = rearE.intValue();
                if (e <= rearS.intValue()) {
                    e += DAY_MIN; // 익일 걸침(V5) — 종전 스케줄 오버나이트 규약과 동일.
                }
                out.add(new int[] { rearS, e });
            }
        }

        return out;
    }

    /**
     * 후방 고정연장을 포함한 그날 점유의 <b>마지막 종료 시각</b>(일 anchor 분, 익일이면 1440 초과).
     * 후방 고정연장이 없거나 환산 불가면 null — 호출부는 기존(소정 기준) 로직을 그대로 쓴다.
     *
     * <p>앱 홈 오버나이트 기준일 판정(J5)용: 후방 고정연장이 있으면 그 종료가 그날 근무의 실제 끝이다.
     */
    public static Integer rearFixedOtEnd(
            String fstSchStrTime, String fstSchEndTime,
            String secSchStrTime, String secSchEndTime,
            String fixedOtStrTime, String fixedOtEndTime) {
        List<int[]> segs = fixedOtSegments(
                fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
                null, null, fixedOtStrTime, fixedOtEndTime);
        if (segs.isEmpty()) {
            return null;
        }
        // 전방을 null 로 넘겼으므로 목록에는 후방 1건만 존재한다.
        return segs.get(segs.size() - 1)[1];
    }

    /** 전방+후방 고정연장 총 분(표기/잠정치 합산용). 미설정이면 0. */
    public static int totalFixedOtMinutes(
            String fstSchStrTime, String fstSchEndTime,
            String secSchStrTime, String secSchEndTime,
            String preFixedOtStrTime, String preFixedOtEndTime,
            String fixedOtStrTime, String fixedOtEndTime) {
        int total = 0;
        for (int[] seg : fixedOtSegments(
                fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
                preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime)) {
            total += seg[1] - seg[0];
        }
        return total;
    }

    /** 소정 1·2구간 중 하나라도 자정을 넘기는지(종료&lt;시작 — "2400" 종료는 1440 으로 비-wrap). */
    private static boolean scheduleWraps(String fstStr, String fstEnd, String secStr, String secEnd) {
        return segmentWraps(fstStr, fstEnd) || segmentWraps(secStr, secEnd);
    }

    private static boolean segmentWraps(String startHhmm, String endHhmm) {
        Integer s = DateTimeUtils.hhmmToMinutes(startHhmm);
        Integer e = DateTimeUtils.schEndToMinutes(endHhmm);
        return s != null && e != null && e < s;
    }
}
