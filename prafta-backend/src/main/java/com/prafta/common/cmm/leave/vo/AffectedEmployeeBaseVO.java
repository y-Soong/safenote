package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 정책 변경 영향 분석(화면 8)의 활성 직원 시뮬레이션 입력 행.
 *
 * <p>정책서: {@code .claude/requests/ref/prafta-017/CLAUDE_CODE_INSTRUCTIONS.md} §9.8 (영향 분석 알고리즘)
 *
 * <p>본 VO는 {@code LeavePolicyMapper.selectActiveUsersForImpact}의 결과 행을 운반한다.
 * TB_USER + TB_SITE_NODE(부서명) 조인 + TB_USER_LEAVE_GRANT 집계(기존 부여/사용)를 한 행에 담는다.
 *
 * <ul>
 *   <li>{@code userCd}    — 사용자 코드 (로그 식별용)</li>
 *   <li>{@code userNm}    — 사용자명 (PII 평문, 관리자 화면 한정 노출)</li>
 *   <li>{@code deptNm}    — 소속 부서명 (TB_SITE_NODE.NODE_NM, 없으면 null)</li>
 *   <li>{@code hireDate}  — 입사일 (YYYYMMDD, null 가능)</li>
 *   <li>{@code currentGrant} — 기존 부여 일수 합계 (활성 부여 GRANT_DAYS 합, 근사)</li>
 *   <li>{@code currentUsed}  — 기존 사용 일수 합계 (활성 부여 USED_DAYS 합, 근사)</li>
 * </ul>
 */
@Getter
@Setter
public class AffectedEmployeeBaseVO {

    /** 사용자 코드 */
    private String userCd;

    /** 사용자명 (PII 평문) */
    private String userNm;

    /** 소속 부서명 (TB_SITE_NODE.NODE_NM) */
    private String deptNm;

    /** 입사일 (YYYYMMDD) */
    private String hireDate;

    /** 기존 부여 일수 합계 (근사) */
    private BigDecimal currentGrant;

    /** 기존 사용 일수 합계 (근사) */
    private BigDecimal currentUsed;
}
