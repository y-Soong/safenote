package com.prafta.platform.location.application.query;

/**
 * 운영자 본인 등록 휴대폰(ENC/HMAC) 조회 쿼리 파라미터(TB_USER).
 */
public record OperatorMblQuery(
    String cmpnyCd
    , String userCd
) {
}
