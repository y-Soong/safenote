package com.prafta.web.subcon.subcon01.application.command;

/**
 * 연동 관계 상태 전이 조건부 UPDATE 커맨드(수락/거부/취소/해지 공용).
 *
 * <p>당사자 조건(수락·거부=TGT, 취소=REQ, 해지=양측)은 매퍼별 고정 WHERE 로 강제하며,
 * actorCmpnyCd/actorUserCd 는 JWT 클레임 도출값만 사용한다.
 */
public record RelationProcessCommand(
    Long relationId
    , String actorCmpnyCd
    , String actorUserCd
    , String comment
){
}
