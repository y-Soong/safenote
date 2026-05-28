package com.prafta.web.user.user01.result;

/**
 * 사용자 입사/고용 기본 정보 조회 결과 (PRAFTA-017-4).
 * hireDate는 화면 표기를 위해 YYYY-MM-DD 포맷으로 반환한다(없으면 null).
 *
 * <p>userCd는 항상 NOT NULL(PK)이라 결과 객체의 non-null 보장용으로 함께 조회한다.
 * (HIRE_DATE/EMPLOYMENT_TYPE가 모두 NULL인 사용자도 MyBatis가 행을 null로 접지 않도록 —
 *  return-instance-for-empty-row 기본 false 회피. 사용자 존재 여부 판정의 정확성 확보.)
 */
public record UserHireInfoResult(
    String userCd
    , String hireDate
    , String employmentType
) {
}
