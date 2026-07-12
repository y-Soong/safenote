package com.prafta.app.leave.leaveflow.application.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * prafta-app-018-A: 연차 사용단위 granularity(SYS025) 계층 산출 SSOT.
 *
 * <p>D2-a (Y) 계층형: 설정 단위는 "허용 최소 단위"이며, 설정 granularity 이하(=설정 단위 + 더 굵은 단위 전부)를 허용한다.
 *   granularity 순서(굵→잘게): FULL_DAY(00) → HALF_DAY(01) → HOUR_2(02) → HOUR_1(03) → MIN_30(04).
 * <p>연차 시간차 환산 개편 LC-06: 반반차(SYS025 '05', 0.25일 고정단위)는 계층 밖의 <b>독립 토글</b>이다.
 *   법정은 tb_leave_usage_policy.ALLOW_QUARTER='Y' 일 때만({@link #withQuarter}), 비법정은 타입
 *   USE_UNIT_TYPE='05' 설정일 때만 허용한다(기존 00~04 설정 타입의 허용 집합은 불변 — 하위호환).
 * <p>도메인 종속(연차 사용단위) 헬퍼이므로 common.util 이 아닌 모듈 내부에 둔다.
 *   018-B 단위 게이팅 검증과 동일 상수를 공유하도록 본 클래스를 단일 출처로 사용한다.
 */
public final class LeaveUnitGranularity {

    private LeaveUnitGranularity() {
    }

    /** 표준 순서(굵→잘게) SYS025 코드. 반반차('05')는 계층 밖 독립 토글이라 미포함(LC-06). */
    private static final List<String> UNIT_ORDER = List.of("00", "01", "02", "03", "04");

    /** 반반차(0.25일 고정단위) SYS025 코드 — LC-06. */
    private static final String UNIT_QUARTER = "05";
    /** 반차 코드 — 반반차 표시 위치 산출용(반차 바로 뒤). */
    private static final String UNIT_HALF = "01";

    /** USAGE_UNIT 문자열(법정 정책값) → SYS025 코드 매핑. */
    private static final Map<String, String> USAGE_UNIT_TO_CODE = Map.of(
              "FULL_DAY", "00"
            , "HALF_DAY", "01"
            , "HOUR_2", "02"
            , "HOUR_1", "03"
            , "MIN_30", "04"
    );

    /** 알 수 없는 값 안전 폴백(종일만 허용). */
    private static final String FALLBACK_CODE = "00";

    /**
     * USAGE_UNIT 문자열(FULL_DAY..MIN_30)을 SYS025 코드(00..04)로 변환.
     * 매핑 불가/널이면 FULL_DAY(00) 폴백.
     */
    public static String usageUnitToCode(String usageUnit) {
        if (usageUnit == null) {
            return FALLBACK_CODE;
        }
        return USAGE_UNIT_TO_CODE.getOrDefault(usageUnit, FALLBACK_CODE);
    }

    /**
     * 설정 SYS025 코드(허용 최소 단위)로부터 허용 코드 목록(계층)을 산출한다.
     *
     * <p>예) settingCode=03(HOUR_1) → [00,01,02,03] (30분 제외).
     *      settingCode=00(FULL_DAY) → [00]. settingCode=04(MIN_30) → [00,01,02,03,04].
     * <p>LC-06: settingCode=05(반반차) → [00,01,05] (종일/반차/반반차 — 시간차 없음).
     *   반반차는 계층 순서에 삽입하지 않는다 — 기존 02/03/04 설정 타입에 반반차가 소급
     *   허용되는 하위호환 파괴를 막기 위한 특례(비법정 타입은 '05' 명시 설정으로만 개방).
     * <p>알 수 없는/널 코드는 [00](FULL_DAY 만) 안전 폴백.
     *
     * @param settingCode 설정 단위 SYS025 코드(두자리)
     * @return 허용 단위 코드 목록(굵→잘게 순)
     */
    public static List<String> allowedUnitsByCode(String settingCode) {
        if (UNIT_QUARTER.equals(settingCode)) {
            // LC-06 특례: 반반차 설정 = 종일 + 반차 + 반반차 (시간차 미허용)
            return List.of("00", "01", "05");
        }
        int idx = (settingCode == null) ? -1 : UNIT_ORDER.indexOf(settingCode);
        if (idx < 0) {
            idx = 0; // 알 수 없는 값은 FULL_DAY 만 허용(안전 폴백)
        }
        return List.copyOf(UNIT_ORDER.subList(0, idx + 1));
    }

    /**
     * LC-06: 법정 연차 허용단위 목록에 반반차('05') 토글을 반영한다.
     *
     * <p>{@code allowQuarter}=true 면 반차('01') 바로 뒤(없으면 종일 뒤)에 '05'를 삽입하고,
     * false 면 원본 그대로 반환한다. tb_leave_usage_policy.ALLOW_QUARTER 는 USAGE_UNIT
     * 계층과 독립인 회사 토글이므로 계층 산출과 분리해 적용한다.
     *
     * @param units       기존 허용 단위 목록(굵→잘게 순, {@link #allowedUnitsByCode} 산출값)
     * @param allowQuarter 반반차 허용 여부(ALLOW_QUARTER='Y')
     * @return 반반차 반영 목록(불변 리스트)
     */
    public static List<String> withQuarter(List<String> units, boolean allowQuarter) {
        if (!allowQuarter || units == null || units.contains(UNIT_QUARTER)) {
            return units;
        }
        List<String> result = new ArrayList<>(units);
        int halfIdx = result.indexOf(UNIT_HALF);
        // 반차 뒤 삽입(표시 순서 굵→잘게 유지). 반차가 없으면(종일만) 맨 뒤.
        result.add((halfIdx >= 0) ? halfIdx + 1 : result.size(), UNIT_QUARTER);
        return List.copyOf(result);
    }
}
