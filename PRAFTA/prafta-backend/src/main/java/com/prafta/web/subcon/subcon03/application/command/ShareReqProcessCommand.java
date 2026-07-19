package com.prafta.web.subcon.subcon03.application.command;

/**
 * 데이터 공유 요청 상태 전이(승인 선점/거부/취소) 커맨드.
 *
 * <p>actorCmpnyCd/actorUserCd 는 토큰 gv_* 에서만 주입한다(T1 Q1 승계).
 * 조건부 UPDATE 의 당사자 조건(승인·거부=PRV, 취소=REQ)은 XML 에서 액션별로 고정한다.
 */
public record ShareReqProcessCommand(
    Long shareReqId
    , String actorCmpnyCd
    , String actorUserCd
    , String comment
){
}
