package com.prafta.common.security.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * HMAC 관련 기능 제공 컴포넌트.
 *
 * 설계 의도:
 * - 서비스 레이어가 키를 전달하지 않도록, 서버 기동 시 한 번 로딩해서 내부 보관
 * - 출력은 DB 저장/전송에 편한 Base64URL(no padding)을 기본 포맷으로 사용
 */
@Component
public class HmacSigner {

    private final byte[] pepperKey;

    public HmacSigner(CryptoProperties props) {
        this.pepperKey = KeyMaterial.fromBase64Url(props.hmacPepper());

        // 안전장치: 최소 권장 길이(32 bytes = 256-bit)
        // 64 bytes(512-bit) 사용을 권장하지만, 최소치라도 강제해 실수 방지
        if (pepperKey.length < 32) {
            throw new IllegalStateException("AUTH pepper is too short. Use >= 32 bytes random key.");
        }
    }

    /**
     * RefreshToken을 DB에 안전하게 저장하기 위한 "서버측 해시" 생성.
     *
     * - refreshToken 원문은 DB에 저장하지 말고
     * - HMAC 결과(Base64URL)만 저장하는 패턴 권장
     *
     * 컨텍스트(cmpnyCd/userId)를 메시지에 섞는 이유:
     * - 동일 refreshToken 값이 우연히 재사용되어도 다른 사용자/회사 컨텍스트면 해시가 달라짐
     * - 토큰-사용자 바인딩이 명확해짐
     */
    public String refreshTokenHash(String refreshToken, String cmpnyCd, String userId) {
        String msg = refreshToken + ":" + cmpnyCd + ":" + userId;
        return hmacSha256Base64Url(msg);
    }

    /**
     * HMAC-SHA256(message) -> Base64URL(no padding) 문자열 반환
     */
    public String hmacSha256Base64Url(String message) {
        byte[] raw = hmacSha256(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /**
     * (선택) 검증용. 타이밍 공격 방지를 위해 상수시간 비교 사용.
     */
    public boolean verifyBase64Url(String message, String expectedBase64Url) {
        String actual = hmacSha256Base64Url(message);
        return constantTimeEquals(actual, expectedBase64Url);
    }

    private byte[] hmacSha256(byte[] messageBytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepperKey, "HmacSHA256"));
            return mac.doFinal(messageBytes);
        } catch (Exception e) {
            // 여기 터지면 보통 환경변수/키 디코딩/알고리즘 환경 문제 → 즉시 실패가 맞음
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}