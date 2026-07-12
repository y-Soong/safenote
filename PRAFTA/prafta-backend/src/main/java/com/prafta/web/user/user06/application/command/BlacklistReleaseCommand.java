package com.prafta.web.user.user06.application.command;

/**
 * 블랙리스트 해제(USE_YN 'Y'→'N') 커맨드.
 *
 * <p>회사 스코프(cmpnyCd) + blacklistId 로만 조건부 UPDATE. updateNo 는 수정자 USER_CD(JWT 도출).
 */
public record BlacklistReleaseCommand(
    String cmpnyCd
    , String blacklistId
    , String updateNo
){
}
