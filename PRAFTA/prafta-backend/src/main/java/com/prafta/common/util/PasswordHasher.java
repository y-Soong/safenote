package com.prafta.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PasswordEncoder encoder;
    private final byte[] pepperBytes; // HMAC 키로 사용
    private final boolean pepperEnabled;
    private final int randomPasswordByteLength;

    public PasswordHasher(
            @Value("${security.password.bcrypt.strength:12}") int strength,
            @Value("${security.password.pepper:}") String pepper,
            @Value("${security.password.random.byte-length:24}") int randomPasswordByteLength
    ) {
        this.encoder = new BCryptPasswordEncoder(strength);
        this.pepperEnabled = pepper != null && !pepper.isEmpty();
        this.pepperBytes = pepperEnabled ? pepper.getBytes(StandardCharsets.UTF_8) : new byte[0];
        this.randomPasswordByteLength = randomPasswordByteLength;
    }

    /* ===== 해시 ===== */
    public String hash(String plainPassword) {
        validate(plainPassword);
        return encoder.encode(applyPepper(plainPassword));
    }

    public boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return encoder.matches(applyPepper(plainPassword), storedHash);
    }

    /* ===== 난수 비밀번호 생성 ===== */
    public String generateRandomHash() {
        String rawPassword = generateRandomPassword();
        return encoder.encode(applyPepper(rawPassword));
    }

    public String generateRandomPassword() {
        byte[] bytes = new byte[randomPasswordByteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /* ===== private ===== */

    /**
     * Pepper 적용: HMAC-SHA256(key=pepper, msg=password)
     * 결과는 항상 32바이트 → Base64 인코딩 시 44자 → BCrypt 72바이트 한계 내
     */
    private String applyPepper(String pw) {
        if (!pepperEnabled) return pw;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(pepperBytes, HMAC_ALGORITHM));
            byte[] hmac = mac.doFinal(pw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().withoutPadding().encodeToString(hmac);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC pepper application failed", e);
        }
    }

    private void validate(String pw) {
        if (pw == null || pw.isBlank()) throw new IllegalArgumentException("password is required");
        if (pw.length() > 128) throw new IllegalArgumentException("password too long");
    }
}