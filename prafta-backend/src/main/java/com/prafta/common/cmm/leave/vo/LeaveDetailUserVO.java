package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 직원별 연차 상세(attd09)의 직원 기본정보 + 집계(매퍼 결과 flat row).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <p>부서명({@code deptNm})은 PII성 표시값(관리자 화면 한정). 직급은 TB_USER에 컬럼이 없어 미표시(D-3).
 * 법적 근속/부여 정책/다음 부여 예정일은 정책 계산 의존이므로 서비스 계층에서 산출한다(D-4).
 *
 * <p>법정/법정외 부여, 사용 합계는 활성 행(STATUS=ACTIVE AND DEL_YN=N) 기준으로 매퍼가 집계한다.
 * 가장 임박한 ACTIVE 법정 만료일({@code legalNearestExpire})은 잔여(부여-사용)가 남은 활성 부여 중
 * AVAIL_TO_DATE 최소값.
 */
@Getter
@Setter
public class LeaveDetailUserVO {

    /** 사용자 코드 */
    private String userCd;

    /** 사용자명 (PII 평문) */
    private String userNm;

    /** 소속 부서명 */
    private String deptNm;

    /** 고용형태[SYS041] */
    private String employmentType;

    /** 입사일 (YYYYMMDD) */
    private String hireDate;

    /** 경력 인정 개월 수 합계 (USE_YN='Y') */
    private Integer creditMonths;

    /** 법정 부여 합계 */
    private java.math.BigDecimal legalGranted;

    /** 법정 사용 합계 */
    private java.math.BigDecimal legalUsed;

    /** 법정외 부여 합계 */
    private java.math.BigDecimal nonLegalGranted;

    /** 법정외 사용 합계 */
    private java.math.BigDecimal nonLegalUsed;

    /** 가장 임박한 ACTIVE 법정 만료일 (AVAIL_TO_DATE, 없으면 null) */
    private String legalNearestExpire;
}
