package com.prafta.web.user.user06.application.command;

/**
 * 블랙리스트 INSERT 커맨드.
 *
 * <p>휴대폰은 평문 저장 금지 — mblNoEnc(AES-GCM)/mblNoHmac(매칭)/mblNoLast4(마스킹)만 적재한다.
 * insertNo 는 등록자 USER_CD(JWT 도출). USE_YN 은 매퍼에서 'Y' 고정.
 */
public record BlacklistInsertCommand(
    String cmpnyCd
    , String blacklistId
    , String mblNoEnc
    , String mblNoHmac
    , String mblNoLast4
    , String reason
    , String insertNo
){
}
