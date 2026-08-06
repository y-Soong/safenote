package com.prafta.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 근무 구간(출퇴근) 시각 겹침 판정 공용 헬퍼.
 *
 * <p>같은 근무일에 서로 다른 WORK_SEQ 구간끼리 시각이 겹치는지를 검사한다(정책서 attd §7.6).
 *   "구간"은 분 stamp(자정 기준 분, {@link DateTimeUtils#toMinuteStamp(String, String, String)})로
 *   표현된 [start, end] 정수 구간이다. 인접 경계(a.end == b.start)는 겹치지 않는 것으로 본다
 *   ({@link IntervalUtils#hasOverlap(List)} 규약 그대로).
 *
 * <p><b>겹침가드 개선(2026-08-06, 사용자 확정 §0-3)</b> — 판정 규칙이 다음과 같이 확장되었다.
 * <ul>
 *   <li><b>D-2 이웃날 open clamp</b>: 퇴근 미입력(open) 구간의 종료를 무조건 +무한대로 보지 않는다.
 *       판정 대상 근무일과 <b>같은 날(dayOffset==0)</b> 의 open 만 {@link #OPEN_END_SENTINEL}(+무한대 근사)로 두고
 *       (같은 날 이중 출근 방지 = §7.6 본래 취지), <b>앞뒤 근무일(dayOffset!=0)</b> 의 open 은
 *       <b>그 구간이 속한 근무일의 다음날 00:00</b>({@code (dayOffset + 2) * 1440})에서 종료시킨다.
 *       즉 미마감 구간은 자기 근무일을 넘어 이웃 근무일을 침범하지 않는다.
 *       (근거: 미마감에는 산입될 실근무 실적이 없고, 퇴근이 채워지는 시점에 CLOSED 로 다시 판정된다.)</li>
 *   <li><b>D-1 손상 구간 제외</b>: 퇴근 stamp ≤ 출근 stamp 인 행은 {@link SegmentKind#CORRUPT} 로 분류해
 *       <b>자기 것이든 남의 것이든 판정에서 제외</b>한다(점유 폭 0). 길이 0 근태
 *       ("출근 누락 → 퇴근 시점에 자리 생성 → 보정 요청")는 정상 운용 산물이므로 차단 대상이 아니다.
 *       <br>단 <b>퇴근 시각은 있는데 퇴근 일자가 없어 stamp 산출이 불가한 행은 {@link SegmentKind#OPEN}</b> 으로 본다
 *       (종료 시각 미상). 이 행은 실근무·주52시간 집계가 근무일 기준으로 환산해 정상 근무로 산입하므로,
 *       겹침 판정에서만 빼면 같은 시간대가 중복 계상된다(2026-08-06 security 지적).</li>
 *   <li><b>진단 API</b>: {@link #findConflict(List)} 가 충돌 상대 구간(날짜/차수/시각/종류)을 돌려주어
 *       원인별 안내 문구({@link AttdOverlapMessages})를 만들 수 있다.</li>
 * </ul>
 *
 * <p>도메인 비종속(분 stamp 만 다룸) + 4개 경로(앱 출근/앱 퇴근/웹 보정승인/웹 직접수정)에서
 *   공통 재사용되므로 {@code com.prafta.common.util} 에 둔다. (출처: 사용자 결정 2026-06-21 / 개정 2026-08-06)
 */
public final class AttdOverlapUtils {

    /**
     * open 구간(퇴근 미입력)의 종료 stamp 근사값(+무한대). 분 stamp 표현 윈도우(0..4320,
     * {@link DateTimeUtils#toStampRange})를 크게 초과하는 값이라 어떤 유한 구간보다도 뒤에 위치한다.
     *
     * <p>2026-08-06 개정 이후 <b>당일(dayOffset==0) open 에만</b> 적용한다(이웃날은 근무일 경계 clamp).
     */
    public static final int OPEN_END_SENTINEL = 1_000_000;

    /** 하루 분 수(분 stamp 축 단위). */
    private static final int MINUTES_PER_DAY = 1440;

    private AttdOverlapUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 구간 종류 3분류(정책서 attd §7.6 개정 2026-08-06). */
    public enum SegmentKind {
        /** 출근·퇴근이 모두 성립(퇴근 > 출근) — 확정 구간. */
        CLOSED,
        /**
         * 종료 시각 미상 — 미마감 구간. 종료는 당일=SENTINEL / 이웃날=근무일 경계.
         * 퇴근 시각·일자가 모두 없는 정상 미마감과, 퇴근 시각은 있으나 일자가 없어 stamp 산출이
         * 불가한 손상 행을 함께 담는다(후자를 CORRUPT 로 빼면 원장 이중 산입이 열린다).
         */
        OPEN,
        /** 퇴근 ≤ 출근(길이 0/역전) — 점유 폭 0으로 보아 판정 제외. */
        CORRUPT
    }

    /**
     * 겹침 판정 단위 구간(신원 + 종류 포함).
     *
     * <p>{@code start}/{@code end} 는 <b>판정 대상 근무일(baseWorkYmd)</b> 기준 분 stamp 다.
     *   {@code dayOffset} 은 이 구간이 속한 근무일(workYmd)과 판정 대상 근무일의 일수 차이다
     *   (행의 WORK_YMD 기준이며 CHECK_IN_DATE 기준이 아니다).
     *
     * @param attdId       근태 ID. <b>null 이면 "아직 저장되지 않은 배치 내 다른 구간"</b>(웹 직접수정 배치)이다.
     * @param workYmd      이 구간이 속한 근무일(yyyyMMdd)
     * @param workSeq      근무 차수(표시용, null 허용)
     * @param dayOffset    (workYmd - baseWorkYmd) 일수 차이
     * @param start        시작 분 stamp
     * @param end          종료 분 stamp(CORRUPT 이면 start 와 동일 — 판정 제외되므로 의미 없음)
     * @param kind         구간 종류
     * @param self         이번에 저장/승인하려는 구간이면 true
     */
    public record Segment(
              String attdId
            , String workYmd
            , String workSeq
            , int dayOffset
            , int start
            , int end
            , SegmentKind kind
            , boolean self
            , String checkInDate
            , String checkInTime
            , String checkOutDate
            , String checkOutTime
    ) {

        /**
         * 겹침 판정 대상인지. CORRUPT(손상)와 폭 0 이하(이웃날 open 이 이미 근무일 경계를 넘긴 경우)는 제외한다.
         */
        public boolean judgeable() {
            return kind != SegmentKind.CORRUPT && end > start;
        }

        /**
         * 아직 저장되지 않은 배치 내 구간(웹 직접수정에서 같이 저장 중인 <b>다른</b> model)인지.
         *
         * <p>{@code self} 구간은 제외한다. 생성요청 승인처럼 {@code excludeAttdId} 가 없으면
         *   self 도 attdId 가 비는데, 그때 "이번 저장 요청의 다른 근무 구간" 이라는 문구가
         *   자기 자신을 가리키게 되기 때문이다(안내 문구는 항상 상대 구간을 지목해야 한다).
         */
        public boolean batchPending() {
            return !self && (attdId == null || attdId.isEmpty());
        }
    }

    /**
     * 겹침 충돌 1쌍. {@code self} 는 이번에 저장/승인하려는 구간(없으면 충돌쌍 중 앞선 구간),
     * {@code other} 는 그 상대 구간(안내 문구의 주어).
     */
    public record Conflict(Segment self, Segment other) {
    }

    /**
     * (출근/퇴근 일자·시각) 한 행을 판정 대상 근무일 기준 {@link Segment} 로 빌드한다.
     *
     * <p>출근 stamp 를 산출할 수 없으면(일자/시각 결측·형식오류) null 을 반환한다(검사 제외).
     *   그 외에는 항상 Segment 를 반환하며, 판정 제외 여부는 {@link Segment#judgeable()} 로 판단한다
     *   (호출자가 CORRUPT 행을 경고 로그로 남길 수 있게 하기 위함).
     *
     * @param baseWorkYmd 판정 대상 근무일(stamp 기준선, yyyyMMdd)
     * @param segWorkYmd  이 행이 속한 근무일(yyyyMMdd). null 이면 baseWorkYmd 와 같은 날로 본다(dayOffset=0).
     * @param attdId      근태 ID(배치 내 미저장 구간이면 null)
     * @param workSeq     근무 차수(표시용, null 허용)
     * @param self        이번에 저장/승인하려는 구간이면 true
     */
    public static Segment buildSegment(String baseWorkYmd, String segWorkYmd,
                                       String attdId, String workSeq,
                                       String inDate, String inTime,
                                       String outDate, String outTime,
                                       boolean self) {
        Integer inStamp = DateTimeUtils.toMinuteStamp(baseWorkYmd, inDate, inTime);
        if (inStamp == null) {
            return null; // 출근 stamp 미확정 → 검사 제외
        }
        int start = inStamp.intValue();
        int dayOffset = resolveDayOffset(baseWorkYmd, segWorkYmd);
        String ownYmd = (segWorkYmd == null || segWorkYmd.isEmpty()) ? baseWorkYmd : segWorkYmd;

        Integer outStamp = DateTimeUtils.toMinuteStamp(baseWorkYmd, outDate, outTime);
        SegmentKind kind;
        int end;
        if (outStamp == null) {
            // 퇴근 stamp 산출 불가 → 종료 시각을 알 수 없는 구간이므로 OPEN 으로 본다.
            //   - 퇴근 시각·일자 모두 없음(정상 미마감)
            //   - 퇴근 시각은 있는데 CHECK_OUT_DATE 가 NULL 인 손상 행
            // ★ 후자를 CORRUPT(판정 제외)로 두면 원장 이중 산입이 열린다(2026-08-06 security 지적).
            //   Attd_11 실근무(Attd11ServiceImpl:113~118)·Attd_15 주52시간(Attd15ServiceImpl:117~123) 집계는
            //   CHECK_OUT_DATE 가 NULL 이면 WORK_YMD 로 폴백해 그 행을 정상 근무로 산입한다.
            //   따라서 겹침 판정에서만 빼면 "집계에는 잡히는데 겹침은 통과" 하는 비대칭이 생긴다.
            //   길이 0(D-1 확정, 아래 out <= in 분기)은 집계도 worked>0 가드로 0분이라 대칭이 유지된다.
            kind = SegmentKind.OPEN;
            // D-2(2026-08-06 확정): 당일 open 만 +무한대. 이웃날 open 은 자기 근무일의 다음날 00:00 에서 종료.
            end = (dayOffset == 0)
                    ? OPEN_END_SENTINEL
                    : (dayOffset + 2) * MINUTES_PER_DAY;
        } else if (outStamp.intValue() <= start) {
            // D-1(2026-08-06 확정): 퇴근 ≤ 출근(길이 0/역전)은 자기 것이든 남의 것이든 판정 제외.
            kind = SegmentKind.CORRUPT;
            end = start;
        } else {
            kind = SegmentKind.CLOSED;
            end = outStamp.intValue();
        }

        return new Segment(attdId, ownYmd, workSeq, dayOffset, start, end, kind, self,
                inDate, inTime, outDate, outTime);
    }

    /**
     * 한 근태 행의 구간 종류만 판정한다(표시 전용 — 승인·상세 화면의 상태 배지).
     * 자기 근무일을 기준선으로 삼으므로 dayOffset=0(당일) 규칙이 적용된다.
     *
     * @return 종류. 출근 stamp 를 산출할 수 없으면 null.
     */
    public static SegmentKind classify(String workYmd,
                                       String inDate, String inTime,
                                       String outDate, String outTime) {
        Segment seg = buildSegment(workYmd, workYmd, null, null, inDate, inTime, outDate, outTime, false);
        return (seg == null) ? null : seg.kind();
    }

    /**
     * 구간 목록에서 겹치는 쌍 1건을 찾아 돌려준다(없으면 null).
     *
     * <ul>
     *   <li>{@link Segment#judgeable()} 가 false 인 구간(CORRUPT / 폭 0 이하)은 판정에서 제외한다.</li>
     *   <li>{@code self=true} 구간이 관여한 충돌을 우선 반환한다(사용자가 지금 하려는 행위의 원인 안내가 목적).</li>
     *   <li>self 무관 충돌만 있으면 그중 1건을 반환한다(기존 {@code hasSegmentOverlap} 의 전역 boolean 의미 보존).</li>
     *   <li>인접 경계(a.end == b.start)는 겹치지 않는 것으로 본다({@link IntervalUtils#hasOverlap(List)} 규약 동일).</li>
     * </ul>
     */
    public static Conflict findConflict(List<Segment> segments) {
        if (segments == null || segments.size() < 2) {
            return null;
        }
        List<Segment> judged = new ArrayList<>();
        for (Segment s : segments) {
            if (s != null && s.judgeable()) {
                judged.add(s);
            }
        }
        judged.sort(Comparator.comparingInt(Segment::start));

        Conflict fallback = null;
        for (int i = 0; i < judged.size(); i++) {
            Segment a = judged.get(i);
            for (int j = i + 1; j < judged.size(); j++) {
                Segment b = judged.get(j);
                // 시작 기준 정렬이므로 b.start >= a.end 면 이후 구간도 a 와 겹칠 수 없다.
                if (b.start() >= a.end()) {
                    break;
                }
                if (a.self()) {
                    return new Conflict(a, b);
                }
                if (b.self()) {
                    return new Conflict(b, a);
                }
                if (fallback == null) {
                    fallback = new Conflict(a, b);
                }
            }
        }
        return fallback;
    }

    /**
     * (workYmd - baseWorkYmd) 일수 차이. 산출 불가(null/형식오류)면 0(같은 날)으로 본다.
     */
    private static int resolveDayOffset(String baseWorkYmd, String segWorkYmd) {
        if (baseWorkYmd == null || baseWorkYmd.length() != 8) return 0;
        if (segWorkYmd == null || segWorkYmd.length() != 8) return 0;
        try {
            return DateTimeUtils.diffDays(segWorkYmd, baseWorkYmd);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * (출근 stamp, 퇴근 stamp) 한 쌍을 겹침 검사용 [start, end] 구간으로 빌드한다.
     *
     * @deprecated 2026-08-06 겹침가드 개선으로
     *     {@link #buildSegment(String, String, String, String, String, String, String, String, boolean)} 로 대체되었다.
     *     이 메서드는 이웃날 clamp(D-2)와 손상행 제외(D-1)를 반영하지 못한다(퇴근 ≤ 출근을 +∞ 로 보정).
     *     미이관 호출부 호환용으로만 남긴다.
     *
     * @param inStamp  출근 분 stamp(null 이면 검사 제외)
     * @param outStamp 퇴근 분 stamp(null 이면 open)
     * @return 길이 2의 [start, end] 구간. 검사 제외 대상이면 null.
     */
    @Deprecated
    public static int[] buildStamp(Integer inStamp, Integer outStamp) {
        if (inStamp == null) {
            return null;
        }
        int start = inStamp.intValue();
        int end = (outStamp == null) ? OPEN_END_SENTINEL : outStamp.intValue();
        if (end <= start) {
            end = OPEN_END_SENTINEL;
        }
        return new int[] { start, end };
    }

    /**
     * 구간 목록에 서로 겹치는 쌍이 있으면 true.
     *
     * @deprecated 2026-08-06 겹침가드 개선으로 {@link #findConflict(List)} 로 대체되었다(원인 진단 불가).
     *     미이관 호출부 호환용으로만 남긴다.
     */
    @Deprecated
    public static boolean hasSegmentOverlap(List<int[]> segments) {
        return IntervalUtils.hasOverlap(segments);
    }
}
