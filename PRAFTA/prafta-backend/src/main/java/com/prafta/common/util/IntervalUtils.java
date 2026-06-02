package com.prafta.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [start, end] 정수 구간 연산을 위한 순수 함수 헬퍼.
 *
 * <p>"구간(interval)"은 길이 2의 {@code int[]}로, index 0은 시작(inclusive),
 * index 1은 종료(exclusive)다 (start &lt; end). 인접 경계(a.end == b.start)는
 * 겹치는 것으로 간주하지 않는다. {@link #merge(List)}는 인접 구간을 하나로
 * 합치지만, {@link #hasOverlap(List)}는 겹치지 않는 것으로 보고한다.
 *
 * <p>입력은 변경하지 않는다. 호출자는 반환되는 리스트를 새로운 객체로 다뤄야 한다.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class IntervalUtils {

    private IntervalUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 시작점 기준으로 정렬한 후 겹치거나 인접한 구간들을 병합하여
     * 새로운 disjoint(서로 겹치지 않는) 정렬 리스트를 반환한다.
     */
    public static List<int[]> merge(List<int[]> segs) {
        List<int[]> sorted = new ArrayList<>(segs);
        sorted.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> out = new ArrayList<>();
        for (int[] cur : sorted) {
            if (out.isEmpty() || out.get(out.size() - 1)[1] < cur[0]) {
                out.add(new int[] { cur[0], cur[1] });
            } else {
                int[] last = out.get(out.size() - 1);
                if (cur[1] > last[1]) last[1] = cur[1];
            }
        }
        return out;
    }

    /**
     * (a - b)를 disjoint 구간 리스트로 반환한다. 두 입력 모두
     * {@link #merge(List)}의 결과(정렬됨, 겹침 없음)여야 한다.
     */
    public static List<int[]> subtract(List<int[]> a, List<int[]> b) {
        List<int[]> out = new ArrayList<>();
        for (int[] seg : a) {
            int cursor = seg[0];
            int end = seg[1];
            for (int[] sub : b) {
                if (sub[1] <= cursor) continue;       // sub가 cursor보다 완전히 앞에 있음
                if (sub[0] >= end) break;             // sub가 end에서 시작하거나 그 뒤에 있음
                if (sub[0] > cursor) {
                    out.add(new int[] { cursor, sub[0] });
                }
                cursor = Math.max(cursor, sub[1]);
                if (cursor >= end) break;
            }
            if (cursor < end) {
                out.add(new int[] { cursor, end });
            }
        }
        return out;
    }

    /**
     * {@code req}가 {@code allowed}의 단일 구간 안에 완전히 포함되는 경우 true를 반환한다.
     * allowed는 {@link #subtract(List, List)}의 결과(정렬됨, 겹침 없음)로 가정한다.
     */
    public static boolean isContainedInAny(int[] req, List<int[]> allowed) {
        for (int[] a : allowed) {
            if (a[0] <= req[0] && a[1] >= req[1]) return true;
        }
        return false;
    }

    /**
     * 두 개 이상의 요청 구간이 서로 겹치는지 검출한다.
     * 인접 경계(a.end == b.start)는 겹치는 것으로 간주하지 않는다.
     */
    public static boolean hasOverlap(List<int[]> segs) {
        List<int[]> sorted = new ArrayList<>(segs);
        sorted.sort(Comparator.comparingInt(a -> a[0]));
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i)[0] < sorted.get(i - 1)[1]) return true;
        }
        return false;
    }
}
