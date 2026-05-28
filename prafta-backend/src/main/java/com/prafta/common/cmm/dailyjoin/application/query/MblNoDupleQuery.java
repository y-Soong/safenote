package com.prafta.common.cmm.dailyjoin.application.query;

/**
 * 일일사용자 회원가입 - 휴대폰번호 중복체크 쿼리.
 * 회사(CMPNY_CD) 스코프 내에서 MBL_NO_HMAC 기준으로 중복을 카운트한다.
 */
public record MblNoDupleQuery(
    String cmpnyCd
    , String mblNoHmac
) {
    public static MblNoDupleQuery of(String cmpnyCd, String mblNoHmac) {
        return new MblNoDupleQuery(cmpnyCd, mblNoHmac);
    }
}
