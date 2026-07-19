package com.prafta.platform.location.application.command;

/**
 * 플랫폼 위치열람 SMS 인증 통과 처리(VERIFIED_YN='Y') 커맨드.
 */
public record PlatformSmsVerifyCommand(
    String smsId
    , String mblNoHmac
    , String certNo
    , String gvUserCd
) {
}
