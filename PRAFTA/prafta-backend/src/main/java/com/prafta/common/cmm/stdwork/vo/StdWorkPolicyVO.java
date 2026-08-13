package com.prafta.common.cmm.stdwork.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 소정-02: 통상근로자 주 소정근로시간 기준값 1행 (TB_CMPNY_STD_WORK_POLICY).
 *
 * <p>스코프는 {@code (SCOPE_TYPE, SCOPE_CD)} 2축이다.
 * <ul>
 *   <li>{@code COMPANY / '-'} — 회사 기본값</li>
 *   <li>{@code SITE / SITE_CD} — 사업장 오버라이드</li>
 * </ul>
 *
 * <p>조회 폴백은 <b>사업장 지정값 → 회사 기본값 → 코드 상수 2400분</b> 3단이며, 어느 단에서
 * 값이 나왔는지를 화면 배지/출처 표기가 구분할 수 있도록 {@code scopeType} 을 함께 싣는다.
 */
@Getter
@Setter
public class StdWorkPolicyVO {

    /** 적용 범위 — COMPANY / SITE */
    private String scopeType;

    /** 범위 코드 — COMPANY 이면 '-', SITE 이면 SITE_CD */
    private String scopeCd;

    /** 통상근로자 주 소정근로 분 */
    private Integer weekStdMinutes;

    /** 사업장 오버라이드 행인지 여부. */
    public boolean isSiteScope() {
        return SCOPE_TYPE_SITE.equals(scopeType);
    }

    /** 적용 범위 코드 — 회사 기본값. */
    public static final String SCOPE_TYPE_COMPANY = "COMPANY";

    /** 적용 범위 코드 — 사업장 오버라이드. */
    public static final String SCOPE_TYPE_SITE = "SITE";

    /** SCOPE_TYPE='COMPANY' 행의 SCOPE_CD 고정값 (PK 컬럼이라 NULL 불가). */
    public static final String SCOPE_CD_COMPANY = "-";
}
