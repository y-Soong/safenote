package com.prafta.platform.location.application.command;

/**
 * 플랫폼 위치열람 목적 SMS 인증코드 INSERT 커맨드.
 *
 * <p>PURPOSE_CD='PLATFORM_LOCATION' 은 XML 에 고정한다(호출부 실수로 목적 혼용 방지).
 * mblNoEnc 는 TB_USER 의 암호화 값을 그대로 복사한다(평문 복호화 없음 — PII 최소 접촉).
 */
public record PlatformSmsAuthInsertCommand(
    String mblNoEnc
    , String mblNoHmac
    , String certNo
    , String gvUserCd
    // SMS-PPURIO-06: 발송 추적키(TB_SMS_AUTH_CODE.SEND_REF_KEY).
    // record 는 setter 가 없어 useGeneratedKeys 로 PK 를 되받을 수 없으므로,
    // INSERT 전에 생성한 refKey 를 함께 저장해 발송 결과 UPDATE 의 조인키로 쓴다.
    , String sendRefKey
    // SMS2-B4: 요청 IP 해시(TB_SMS_AUTH_CODE.SEND_IP_HASH). IP 축 상한 카운트 재료.
    // ★평문 IP 가 아니라 HMAC 해시다(공통 정책서 §11.1). 확정 불가 시 null(fail-open).
    // ★사용자 축(SEND_USER_CD)은 gvUserCd 를 그대로 재사용한다(XML 참조).
    , String sendIpHash
) {
}
