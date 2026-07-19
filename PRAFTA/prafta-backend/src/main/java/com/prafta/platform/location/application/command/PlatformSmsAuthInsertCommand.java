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
) {
}
