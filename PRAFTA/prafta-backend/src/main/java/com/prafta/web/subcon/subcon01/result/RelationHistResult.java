package com.prafta.web.subcon.subcon01.result;

/**
 * 연동 관계 이력 응답 1행.
 *
 * <p>행위자 표기(Q4 확정): 자사 소속만 실명, 상대사 행위자는 "상대사 처리" 고정 라벨.
 */
public record RelationHistResult(
    Long histId
    , String actionType
    , String actionUserNm
    , String actionDtime
    , String actionDesc
){
}
