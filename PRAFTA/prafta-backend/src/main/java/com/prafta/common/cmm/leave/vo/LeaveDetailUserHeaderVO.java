package com.prafta.common.cmm.leave.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 직원별 연차 상세(attd09) 헤더 정보(응답용, 가공 완료).
 *
 * <p>{@code tenureText}(실제 근속) / {@code grantPolicyText}(활성 정책 라벨) /
 * {@code nextGrantDateText}(다음 부여 예정일, 근사)는 D-4 결정에 따라 서비스에서 산출하며,
 * 산출 불가 시 "-"를 담는다. 직급은 TB_USER에 컬럼이 없어 미표시(D-3).
 */
@Getter
@Builder
public class LeaveDetailUserHeaderVO {

    /** 사용자 코드 */
    private final String userCd;

    /** 사용자명 (PII 평문) */
    private final String userNm;

    /** 소속 부서명 */
    private final String deptNm;

    /** 고용형태[SYS041] */
    private final String employmentType;

    /** 입사일 (YYYYMMDD) */
    private final String hireDate;

    /** 실제 근속 텍스트 (예 "8년 2개월", 입사일 없으면 "-") */
    private final String tenureText;

    /** 부여 정책 라벨 (활성 정책 AXIS1: 입사일기준/회계연도기준, 없으면 "-") */
    private final String grantPolicyText;

    /** 다음 부여 예정일 텍스트 (근사, 산출 불가 시 "-") */
    private final String nextGrantDateText;
}
