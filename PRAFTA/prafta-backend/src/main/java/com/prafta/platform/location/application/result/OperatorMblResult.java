package com.prafta.platform.location.application.result;

/**
 * 운영자 본인 등록 휴대폰 조회 결과(TB_USER — 암호화/HMAC 값만, 평문 없음).
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — XML SELECT 순서와 일치 유지.
 */
public record OperatorMblResult(
    String mblNoEnc
    , String mblNoHmac
) {
}
