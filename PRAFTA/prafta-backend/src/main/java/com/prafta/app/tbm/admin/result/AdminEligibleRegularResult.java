package com.prafta.app.tbm.admin.result;

/**
 * E9 정규직 대리입실 후보 행(prafta-051 R-B).
 *
 * <p>PII 최소: 사용자코드/이름/소속(노드명)만. 휴대폰/이메일 등 민감정보는 노출하지 않는다.
 * alreadyEntered=true 면 해당 세션에 이미 입실(DEL_YN='N' 출결 존재) → 프론트가 비활성 표시.
 */
public record AdminEligibleRegularResult(
    String userCd
    , String userNm
    , String deptNm
    , boolean alreadyEntered
){
}
