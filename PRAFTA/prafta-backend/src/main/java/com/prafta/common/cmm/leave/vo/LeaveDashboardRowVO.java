package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 현황 대시보드(attd09) 직원 1행의 매퍼 결과(flat row).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <p>법정/법정외 부여, 사용 합계는 {@code tb_user_leave_grant}의 활성 행
 * (STATUS=ACTIVE AND DEL_YN=N)을 GRANT_TYPE prefix(STATUTORY_ / MANUAL_)로 구분 집계한다.
 * 잔여(remaining)와 사용률(usageRate), 근속 텍스트는 서비스 계층에서 산출하므로 본 VO에는 없다.
 *
 * <p>{@code userNm}/{@code deptNm}은 PII 평문(관리자 화면 한정 노출). 로그에는 userCd만 남긴다.
 */
@Getter
@Setter
public class LeaveDashboardRowVO {

    /** 사용자 코드 */
    private String userCd;

    /** 사용자명 (PII 평문) */
    private String userNm;

    /** 소속 부서명 (TB_SITE_NODE.NODE_NM, 없으면 null) */
    private String deptNm;

    /** 입사일 (YYYYMMDD, null 가능) */
    private String hireDate;

    /** 고용형태[SYS041] REGULAR/CONTRACT/DAILY/EXECUTIVE (null 가능) */
    private String employmentType;

    /** 경력 인정 개월 수 합계 (tb_user_service_credit, USE_YN='Y') */
    private Integer creditMonths;

    /** 법정 부여 일수 합계 (GRANT_TYPE LIKE 'STATUTORY\_%') */
    private BigDecimal legalGranted;

    /** 법정 사용 일수 합계 */
    private BigDecimal legalUsed;

    /** 법정 사용예정 일수 합계 (CONFIRMED 사용 중 START_DATE > 오늘 = 아직 도래하지 않은 미래분) */
    private BigDecimal legalScheduled;

    /** 법정외 부여 일수 합계 (GRANT_TYPE LIKE 'MANUAL\_%') */
    private BigDecimal nonLegalGranted;

    /** 법정외 사용 일수 합계 */
    private BigDecimal nonLegalUsed;

    /** 법정외 사용예정 일수 합계 (CONFIRMED 사용 중 START_DATE > 오늘 = 아직 도래하지 않은 미래분) */
    private BigDecimal nonLegalScheduled;
}
