package com.prafta.web.user.user06.result;

/**
 * 블랙리스트 목록 매퍼 원시 결과 1행.
 *
 * <p>휴대폰(mblNoEnc)은 AES-GCM 암호문, mblNoLast4 는 마지막4자리. 서비스단에서 마스킹 문자열로 변환한다.
 * 암호문(mblNoEnc)은 프론트로 노출하지 않는다. insertNm 은 등록자(INSERT_NO=USER_CD) 이름 조인값.
 */
public record BlacklistRaw(
    String blacklistId
    , String mblNoEnc
    , String mblNoLast4
    , String reason
    , String useYn
    , String insertNm
    , String insertDate
){
}
