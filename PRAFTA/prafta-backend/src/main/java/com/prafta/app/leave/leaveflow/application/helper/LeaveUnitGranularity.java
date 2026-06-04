package com.prafta.app.leave.leaveflow.application.helper;

import java.util.List;
import java.util.Map;

/**
 * prafta-app-018-A: 연차 사용단위 granularity(SYS025) 계층 산출 SSOT.
 *
 * <p>D2-a (Y) 계층형: 설정 단위는 "허용 최소 단위"이며, 설정 granularity 이하(=설정 단위 + 더 굵은 단위 전부)를 허용한다.
 *   granularity 순서(굵→잘게): FULL_DAY(00) → HALF_DAY(01) → HOUR_2(02) → HOUR_1(03) → MIN_30(04).
 * <p>도메인 종속(연차 사용단위) 헬퍼이므로 common.util 이 아닌 모듈 내부에 둔다.
 *   018-B 단위 게이팅 검증과 동일 상수를 공유하도록 본 클래스를 단일 출처로 사용한다.
 */
public final class LeaveUnitGranularity {

    private LeaveUnitGranularity() {
    }

    /** 표준 순서(굵→잘게) SYS025 코드. */
    private static final List<String> UNIT_ORDER = List.of("00", "01", "02", "03", "04");

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
     * <p>알 수 없는/널 코드는 [00](FULL_DAY 만) 안전 폴백.
     *
     * @param settingCode 설정 단위 SYS025 코드(두자리)
     * @return 허용 단위 코드 목록(굵→잘게 순)
     */
    public static List<String> allowedUnitsByCode(String settingCode) {
        int idx = (settingCode == null) ? -1 : UNIT_ORDER.indexOf(settingCode);
        if (idx < 0) {
            idx = 0; // 알 수 없는 값은 FULL_DAY 만 허용(안전 폴백)
        }
        return List.copyOf(UNIT_ORDER.subList(0, idx + 1));
    }
}
