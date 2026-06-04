package com.prafta.app.leave.leaveflow.result;

/**
 * prafta-app-018-A: 결재자 검색 1행 (approver-search).
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 *   {@code AppLeaveFlowMapper.searchApprovers} 의 SELECT 절과 1:1.
 * <p>PII 최소노출: 휴대폰/이메일/생년/정렬보조(rankCd·sortIdx)는 SELECT 하지 않는다.
 *   userId(로그인ID) 노출은 결재자 식별 최소범위로 허용(mypage01 동일).
 */
public record ApproverRow(
      String userCd
    , String userId
    , String userNm
    , String rankNm
    , String nodeNm
) {
}
