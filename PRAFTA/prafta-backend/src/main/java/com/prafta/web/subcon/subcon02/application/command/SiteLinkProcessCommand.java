package com.prafta.web.subcon.subcon02.application.command;

/**
 * 사업장 연동 링크 상태 전이(수락/거부/취소/해지) 커맨드.
 *
 * <p>actorCmpnyCd/actorUserCd 는 토큰 gv_* 에서만 주입한다(T1 Q1 승계).
 * 조건부 UPDATE 의 당사자 조건은 XML 에서 상태별로 고정한다.
 */
public record SiteLinkProcessCommand(
    Long linkId
    , String actorCmpnyCd
    , String actorUserCd
    , String comment
){
}
