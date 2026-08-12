package com.prafta.common.cmm.attd.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;

/**
 * PRAFTA-FIXEDOT-3: 고정연장근무 <b>자동 계상 + 후방 미이행 판정</b>의 파생 계산 단일 출처.
 *
 * <p>정책 ①(2026-08-11 확정): 고정연장 실적(분) = 실근태 스탬프 구간들 ∩ 고정연장 구간들의 분 합
 * — <b>실근태가 커버한 분량만</b> 계상한다(약정분 전액 보장은 급여 축 규칙 — 시간 버킷과 분리,
 * 본 유틸에 삭감/보정 로직을 넣지 않는다). 근태 수정/보정 승인 시 원장 동기화가 필요 없도록
 * <b>저장하지 않고 조회 시점마다 파생 계산</b>한다(plan §3-① — 신규 테이블/원장 금지).
 *
 * <p>좌표계: <b>일 anchor(해당 근무일 00:00 = 0분)</b>. 고정연장 구간 환산은
 * {@link FixedOtScheduleUtils#fixedOtSegments} 단일 출처(자정 넘김 +1440, 소정 wrap 시 후방 익일 배치)
 * 를 그대로 쓰고, 실근태 스탬프는 {@link #dayAnchorMinutes} 로 같은 프레임에 정렬한다.
 * 이웃날 실근태(전일 출근/익일 퇴근)는 음수/1440 초과 분으로 자연스럽게 표현되어 교집합 산식이 그대로 성립한다.
 *
 * <p>★NULL 가드: 고정연장이 없는 근무타입은 항상 빈 구간 → 실적 0 / 미이행 false — 전 소비처의
 * 기존(NULL) 경로가 완전히 불변이다.
 *
 * <p>소비처: 웹 Attd_07(일자상세)·Attd_08·Attd_11·Attd_15(주52 연장 축)·앱 관리자 근태·
 * 하도급 공유 스냅샷(Subcon03). 추후 수당 시간 버킷(소정 지시서 3단계)도 본 유틸 산출을 원장에 적재한다.
 */
public final class FixedOtMinutesUtils {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FixedOtMinutesUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * (근무일, 실제 일자, HHmm) → 근무일 00:00 기준 분(일 anchor). 전일이면 음수, 익일이면 1440 초과.
     *
     * <p>{@code ymd} 가 null/공백이면 근무일 당일로 간주한다(근태 스탬프의 CHECK_*_DATE NULL 규약과 동일).
     * 파싱 불가 시 null — 호출부는 그 구간을 계상에서 제외한다(추정 금지).
     */
    public static Integer dayAnchorMinutes(String workYmd, String ymd, String hhmm) {
        if (workYmd == null || workYmd.length() != 8 || hhmm == null || hhmm.length() != 4) {
            return null;
        }
        String baseYmd = (ymd == null || ymd.isBlank()) ? workYmd : ymd;
        if (baseYmd.length() != 8) {
            return null;
        }
        try {
            long dayDiff = LocalDate.parse(baseYmd, YMD).toEpochDay()
                    - LocalDate.parse(workYmd, YMD).toEpochDay();
            int hh = Integer.parseInt(hhmm.substring(0, 2));
            int mm = Integer.parseInt(hhmm.substring(2, 4));
            if (hh < 0 || hh > 24 || mm < 0 || mm > 59) {
                // "2400"(자정 표기)은 스케줄 종료 전용이지만 방어적으로 24:00 까지 허용한다.
                return null;
            }
            return (int) (dayDiff * 1440L + hh * 60L + mm);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 실근태 슬롯 구간(출근~퇴근, 일 anchor 분) 1건을 만든다. 출근/퇴근 어느 한쪽이 없거나(미퇴근 등)
     * 역전(퇴근&lt;=출근)이면 null — 그 슬롯은 계상 대상이 아니다.
     */
    public static int[] actualSegment(Integer inAnchorMin, Integer outAnchorMin) {
        if (inAnchorMin == null || outAnchorMin == null || outAnchorMin <= inAnchorMin) {
            return null;
        }
        return new int[] { inAnchorMin, outAnchorMin };
    }

    /**
     * 고정연장 실적(분) = Σ (실근태 슬롯 구간 ∩ 고정연장 구간) — 정책 ① (b) 커버분만.
     *
     * <p>슬롯 구간들은 서로 겹치지 않는다는 근태 저장 규약(겹침 가드)을 전제로 단순 합산한다.
     * null 슬롯(미완결)은 건너뛴다. 고정연장 구간이 없으면 항상 0(기존 타입 무회귀).
     *
     * @param fixedOtSegs {@link FixedOtScheduleUtils#fixedOtSegments} 산출(일 anchor [start,end))
     * @param actualSegs  {@link #actualSegment} 산출 목록(null 요소 허용 — skip)
     */
    public static int coveredMinutes(List<int[]> fixedOtSegs, List<int[]> actualSegs) {
        if (fixedOtSegs == null || fixedOtSegs.isEmpty() || actualSegs == null || actualSegs.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int[] act : actualSegs) {
            if (act == null) {
                continue;
            }
            for (int[] fx : fixedOtSegs) {
                int s = Math.max(act[0], fx[0]);
                int e = Math.min(act[1], fx[1]);
                if (e > s) {
                    total += (e - s);
                }
            }
        }
        return total;
    }

    /**
     * 그날 고정연장 실적(분) — 스케줄 원시 시각(HHMM 8인자)에서 구간을 환산해 {@link #coveredMinutes} 한다.
     * 고정연장 미설정 타입은 항상 0.
     */
    public static int dayFixedOtActualMinutes(
            String fstSchStrTime, String fstSchEndTime,
            String secSchStrTime, String secSchEndTime,
            String preFixedOtStrTime, String preFixedOtEndTime,
            String fixedOtStrTime, String fixedOtEndTime,
            List<int[]> actualSegs) {
        List<int[]> fixedOtSegs = FixedOtScheduleUtils.fixedOtSegments(
                fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
                preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime);
        return coveredMinutes(fixedOtSegs, actualSegs);
    }

    /**
     * 후방 고정연장 종료(일 anchor 분). 후방 미설정/환산 불가면 null.
     * {@link FixedOtScheduleUtils#rearFixedOtEnd} 위임(소정 wrap 타입의 익일 배치 포함).
     */
    public static Integer rearFixedOtEndAnchor(
            String fstSchStrTime, String fstSchEndTime,
            String secSchStrTime, String secSchEndTime,
            String fixedOtStrTime, String fixedOtEndTime) {
        return FixedOtScheduleUtils.rearFixedOtEnd(
                fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
                fixedOtStrTime, fixedOtEndTime);
    }

    /**
     * "연장 미이행"(후방) 판정 — 정책 ②: 후방 고정연장 존재 + 마지막 슬롯 퇴근 스탬프 &lt; 후방 종료.
     *
     * <p>조퇴 판정(소정 종료 기준)과 <b>완전히 분리된 별도 배지</b>다 — 조퇴 통계/판정 로직은 이 판정과
     * 어떤 상태도 공유하지 않는다. 호출부 책임(정책 ③): 그날 연차 계열(종일/반차/시간차) 확정 사용이
     * 존재하면 이 판정을 호출하지 않거나 결과를 버린다(의무 면제 — 배지 발화 금지. 단 실적 계상은
     * {@link #coveredMinutes} 로 그대로 동작 — "의무 면제, 근무하면 실적 계상" 단일 규칙).
     *
     * <p>미퇴근(lastOutAnchorMin null)·후방 미설정(rearEndAnchorMin null)은 배지 비대상(false).
     *
     * <p>TODO(developer): 전방 고정연장 미이행(늦은 출근으로 전방 구간 미커버) 배지는 1차 제외
     * (plan §5-3 확정 — 후속 확장). 전방 실사용 타입 등장 시 {@code isFrontUnfulfilled}
     * (출근 스탬프 &gt; 전방 시작) 판정을 본 유틸에 추가하고 소비처 배지를 확장할 것.
     */
    public static boolean isRearUnfulfilled(Integer rearEndAnchorMin, Integer lastOutAnchorMin) {
        return rearEndAnchorMin != null && lastOutAnchorMin != null
                && lastOutAnchorMin < rearEndAnchorMin;
    }

    /** 실근태 슬롯 구간 목록 빌더 — (출근일자/시각, 퇴근일자/시각) 쌍들을 일 anchor 구간으로 환산. */
    public static List<int[]> buildActualSegments(String workYmd, String[][] inOutPairs) {
        List<int[]> out = new ArrayList<>(2);
        if (inOutPairs == null) {
            return out;
        }
        for (String[] p : inOutPairs) {
            if (p == null || p.length != 4) {
                continue;
            }
            int[] seg = actualSegment(
                    dayAnchorMinutes(workYmd, p[0], p[1]),
                    dayAnchorMinutes(workYmd, p[2], p[3]));
            if (seg != null) {
                out.add(seg);
            }
        }
        return out;
    }
}
