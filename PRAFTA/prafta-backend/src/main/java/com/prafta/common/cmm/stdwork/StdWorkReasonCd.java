package com.prafta.common.cmm.stdwork;

import java.util.Set;

/**
 * 소정-02: 소정근로시간 사유코드[SYS083] 상수 + 사유 성격 판정.
 *
 * <p>코드값 자체는 마이그레이션 {@code sojeong-1-4-sys083-std-work-reason-seed.sql} 의
 * tb_syst_val_d 시드가 원천이며, 본 클래스는 <b>검증 분기에 필요한 최소 상수</b>만 보유한다.
 *
 * <p>★하드코딩 경계 (plan §1.4)
 * <ul>
 *   <li>2단계의 <b>차감·부여 규칙 분기는 절대 여기에 두지 않는다.</b> 그 규칙은
 *       SYS083 의 {@code VAL_D_INFO_1}(종일 차감) / {@code VAL_D_INFO_2}(부여) 데이터를
 *       읽어 분기한다 — 행정해석 변동을 UPDATE 1건으로 흡수하기 위함.
 *       (조회 진입점: {@code StdWorkHoursService#findReasonRule})</li>
 *   <li>여기 남는 것은 "단축 사유 여부"(종료일 필수 검증)와 "육아기 범위 경고" 처럼
 *       0단계 입력 검증에만 쓰이는 분기다. 사유가 추가되면 시드 + 본 집합만 확장한다.</li>
 * </ul>
 */
public final class StdWorkReasonCd {

    private StdWorkReasonCd() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 통상근로자 (기본값). */
    public static final String NORMAL = "NORMAL";

    /** 단시간 근로계약 (근기법 2조1항9호). */
    public static final String PART_TIME = "PART_TIME";

    /** 육아기 근로시간 단축. */
    public static final String CHILDCARE = "CHILDCARE";

    /** 임신기 근로시간 단축. */
    public static final String PREGNANCY = "PREGNANCY";

    /** 가족돌봄 등 근로시간 단축. */
    public static final String FAMILY_CARE = "FAMILY_CARE";

    /**
     * 단축 근무 사유 집합.
     *
     * <p>단축은 본질적으로 한시 조치이므로 적용 종료일이 필수다(plan §1.1 — "모수(단축 전
     * 소정)의 단일 출처 = 직전 행" 구조가 이 제약으로 성립한다). OT 게이트(소정-07)의
     * 1차 대상 집합이기도 하다.
     */
    private static final Set<String> REDUCED_REASONS = Set.of(CHILDCARE, PREGNANCY, FAMILY_CARE);

    /** 단축 근무 사유(육아기·임신기·가족돌봄)인지 여부. */
    public static boolean isReduced(String reasonCd) {
        return reasonCd != null && REDUCED_REASONS.contains(reasonCd);
    }

    /** 육아기 근로시간 단축인지 여부 (주 15~35시간 범위 경고 대상). */
    public static boolean isChildcare(String reasonCd) {
        return CHILDCARE.equals(reasonCd);
    }
}
