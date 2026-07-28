package com.prafta.app.leave.leaveflow.application.helper;

import java.util.List;
import java.util.Map;

/**
 * prafta-app-018-A: 연차 사용단위 granularity(SYS025) 계층 산출 SSOT.
 *
 * <p>D2-a (Y) 계층형: 설정 단위는 "허용 최소 단위"이며, 설정 granularity 이하(=설정 단위 + 더 굵은 단위 전부)를 허용한다.
 *   granularity 순서(굵→잘게): FULL_DAY(00) → HALF_DAY(01) → HOUR_2(02) → HOUR_1(03) → MIN_30(04).
 * <p>LC-10: 반반차(SYS025 '05', 0.25일 고정단위)는 <b>사용 단위 선택지의 하나</b>다(구 ALLOW_QUARTER
 *   독립 토글 폐기). 법정은 tb_leave_usage_policy.USAGE_UNIT='QUARTER_DAY', 비법정은 타입
 *   USE_UNIT_TYPE='05' 설정일 때만 허용하며, 양쪽 모두 허용집합은 [00,01,05](종일/반차/반반차)로 같다.
 *   '05'는 UNIT_ORDER(계층)에 넣지 않는다 — 기존 02/03/04 설정에 반반차가 소급 허용되는
 *   하위호환 파괴를 막기 위한 특례.
 * <p>도메인 종속(연차 사용단위) 헬퍼이므로 common.util 이 아닌 모듈 내부에 둔다.
 *   018-B 단위 게이팅 검증과 동일 상수를 공유하도록 본 클래스를 단일 출처로 사용한다.
 */
public final class LeaveUnitGranularity {

    private LeaveUnitGranularity() {
    }

    /** 계층 순서(굵→잘게) SYS025 코드. 반반차('05')는 계층 밖 특례라 미포함(LC-10). */
    private static final List<String> UNIT_ORDER = List.of("00", "01", "02", "03", "04");

    /** 반반차(0.25일 고정단위) SYS025 코드 — LC-10. */
    private static final String UNIT_QUARTER = "05";

    /** USAGE_UNIT 문자열(법정 정책값) → SYS025 코드 매핑. */
    private static final Map<String, String> USAGE_UNIT_TO_CODE = Map.of(
              "FULL_DAY", "00"
            , "HALF_DAY", "01"
            , "QUARTER_DAY", "05"
            , "HOUR_2", "02"
            , "HOUR_1", "03"
            , "MIN_30", "04"
    );

    /** 알 수 없는 값 안전 폴백(종일만 허용). */
    private static final String FALLBACK_CODE = "00";

    /**
     * USAGE_UNIT 문자열(FULL_DAY/HALF_DAY/QUARTER_DAY/HOUR_2/HOUR_1/MIN_30)을 SYS025 코드로 변환.
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
     * <p>LC-10: settingCode=05(반반차) → [00,01,05] (종일/반차/반반차 — 시간차 없음).
     *   반반차는 계층 순서에 삽입하지 않는다 — 기존 02/03/04 설정에 반반차가 소급 허용되는
     *   하위호환 파괴를 막기 위한 특례(법정 USAGE_UNIT='QUARTER_DAY' / 비법정 USE_UNIT_TYPE='05' 명시 설정으로만 개방).
     * <p>알 수 없는/널 코드는 [00](FULL_DAY 만) 안전 폴백.
     *
     * @param settingCode 설정 단위 SYS025 코드(두자리)
     * @return 허용 단위 코드 목록(굵→잘게 순)
     */
    public static List<String> allowedUnitsByCode(String settingCode) {
        if (UNIT_QUARTER.equals(settingCode)) {
            // LC-10 특례: 반반차 설정 = 종일 + 반차 + 반반차 (시간차 미허용)
            return List.of("00", "01", "05");
        }
        int idx = (settingCode == null) ? -1 : UNIT_ORDER.indexOf(settingCode);
        if (idx < 0) {
            idx = 0; // 알 수 없는 값은 FULL_DAY 만 허용(안전 폴백)
        }
        return List.copyOf(UNIT_ORDER.subList(0, idx + 1));
    }
}
