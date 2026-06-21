package com.prafta.common.util;

import java.util.List;

/**
 * 근무 구간(출퇴근) 시각 겹침 판정 공용 헬퍼.
 *
 * <p>같은 근무일에 서로 다른 WORK_SEQ 구간끼리 시각이 겹치는지를 검사한다(정책서 attd §7.6).
 *   "구간"은 분 stamp(자정 기준 분, {@link DateTimeUtils#toMinuteStamp(String, String, String)})로
 *   표현된 [start, end] 정수 구간이다. 인접 경계(a.end == b.start)는 겹치지 않는 것으로 본다
 *   ({@link IntervalUtils#hasOverlap(List)} 규약 그대로).
 *
 * <p>"open(퇴근 미입력) 구간"의 종료는 {@link #OPEN_END_SENTINEL}(+무한대 근사)로 본다.
 *   "출근(start) stamp 가 null"인 구간은 시각을 확정할 수 없으므로 검사 대상에서 제외한다
 *   ({@link #buildStamp(Integer, Integer)} 가 null 을 반환 → 호출자가 skip).
 *
 * <p>도메인 비종속(분 stamp 만 다룸) + 4개 경로(앱 출근/앱 퇴근/웹 보정승인/웹 직접수정)에서
 *   공통 재사용되므로 {@code com.prafta.common.util} 에 둔다. (출처: 사용자 결정 2026-06-21)
 */
public final class AttdOverlapUtils {

    /**
     * open 구간(퇴근 미입력)의 종료 stamp 근사값(+무한대). 분 stamp 표현 윈도우(0..4320,
     * {@link DateTimeUtils#toStampRange})를 크게 초과하는 값이라 어떤 유한 구간보다도 뒤에 위치한다.
     */
    public static final int OPEN_END_SENTINEL = 1_000_000;

    private AttdOverlapUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * (출근 stamp, 퇴근 stamp) 한 쌍을 겹침 검사용 [start, end] 구간으로 빌드한다.
     *
     * <ul>
     *   <li>출근(in) stamp 가 null → 시각 미확정 구간이므로 null 반환(호출자가 검사 제외).</li>
     *   <li>퇴근(out) stamp 가 null → open 구간으로 보고 종료를 {@link #OPEN_END_SENTINEL} 로 채운다.</li>
     *   <li>out &lt;= in (자정 미보정 등으로 종료가 시작보다 앞/같음) → end 를 SENTINEL 로 보정해
     *       최소한 [in, +∞) 로 취급한다(겹침을 누락하지 않는 보수적 처리).</li>
     * </ul>
     *
     * @param inStamp  출근 분 stamp(null 이면 검사 제외)
     * @param outStamp 퇴근 분 stamp(null 이면 open)
     * @return 길이 2의 [start, end] 구간. 검사 제외 대상이면 null.
     */
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
     * <p>{@link IntervalUtils#hasOverlap(List)} 의 얇은 래퍼다(인접 경계 허용). 호출자는
     *   {@link #buildStamp(Integer, Integer)} 로 만든 (null 제외) 구간들을 모아서 넘긴다.
     */
    public static boolean hasSegmentOverlap(List<int[]> segments) {
        return IntervalUtils.hasOverlap(segments);
    }
}
