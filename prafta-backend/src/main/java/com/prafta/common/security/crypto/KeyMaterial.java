package com.prafta.common.security.crypto;

import java.util.Base64;

/**
 * 키/비밀값 문자열을 실제 바이트 배열로 변환하는 유틸.
 *
 * 핵심:
 * - ENV에는 사람이 읽는 문장이 아니라 "랜덤 바이트"를 Base64URL로 인코딩해 넣는다.
 * - 서버에서 디코딩하여 byte[]로 사용해야 의도한 보안 강도를 얻는다.
 */
public final class KeyMaterial {

    private KeyMaterial() {}

    /**
     * Base64URL(no padding) 문자열을 byte[]로 디코딩.
     * 예: PowerShell로 생성한 b64url 값을 그대로 넣는 경우에 사용.
     */
    public static byte[] fromBase64Url(String base64UrlNoPadding) {
        if (base64UrlNoPadding == null || base64UrlNoPadding.isBlank()) {
            // 키가 없으면 "기동 실패"가 맞다 (운영에서 조용히 지나가면 사고)
            throw new IllegalStateException("PRAFTA_AUTH_PEPPER is missing (auth.pepper)");
        }
        return Base64.getUrlDecoder().decode(base64UrlNoPadding);
    }
}