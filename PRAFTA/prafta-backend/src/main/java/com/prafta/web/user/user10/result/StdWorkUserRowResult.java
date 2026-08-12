package com.prafta.web.user.user10.result;

/**
 * 소정-10: 소정근로시간 관리 대상 근로자 1행 (오늘 기준 유효 이력 조인 결과).
 *
 * <p>{@code weekStdMinutes} 가 <b>null 이면 이력 미입력</b> 계정이다 — 화면이
 * "미입력(통상 기준 간주)" 배지를 그리는 근거다(폴백 실태를 감추지 않는다, plan UI-C).
 * 폴백 해석값을 여기서 채우지 않는 이유가 바로 그것이다.
 *
 * <p>★record 컬럼 순서 = 매퍼 SELECT 순서와 1:1 (MyBatis 위치 매핑).
 */
public record StdWorkUserRowResult(
        String userCd
        , String userId
        , String userNm
        , String nodeCd
        , String nodeNm
        , String employmentType
        , String hireDate
        , Integer weekStdMinutes
        , String reasonCd
        , String reasonNm
        , String applyStrDate
        , String applyEndDate
) {
}
