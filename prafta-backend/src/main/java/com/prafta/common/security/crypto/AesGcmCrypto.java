package com.prafta.common.security.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * AES-GCM 기반 양방향 암복호 컴포넌트.
 *
 * 저장 포맷(문자열):
 *   v1.<Base64URL(payload)>
 *
 * payload 바이너리 구조:
 *   [1byte version][12byte nonce][ciphertext+tag]
 *
 * - nonce: 12 bytes 권장(GCM 표준)
 * - tag: 16 bytes(128-bit) 권장
 * - key: 32 bytes(256-bit) 권장
 */
@Component
public class AesGcmCrypto {

    private static final byte VERSION_V1 = 1;
    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCrypto(CryptoProperties props) {
        byte[] key = KeyMaterial.fromBase64Url(props.aesKey());

        // AES-256 권장: 32 bytes
        if (key.length != 32) {
            throw new IllegalStateException("AES key must be 32 bytes (AES-256). Check PRAFTA_AES_DATA_KEY.");
        }
        this.keySpec = new SecretKeySpec(key, "AES");
    }

    /** 평문 > 암호문 문자열(v1...) */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;

        try {
            byte[] nonce = new byte[NONCE_LEN];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LEN_BITS, nonce));

            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(1 + NONCE_LEN + ct.length);
            buf.put(VERSION_V1);
            buf.put(nonce);
            buf.put(ct);

            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());
            return "v1." + payload;
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    /** 암호문 문자열(v1...) -> 평문 */
    public String decrypt(String encrypted) {
        if (encrypted == null) return null;

        try {
            if (!encrypted.startsWith("v1.")) {
                throw new IllegalArgumentException("Unsupported encrypted format");
            }

            byte[] payload = Base64.getUrlDecoder().decode(encrypted.substring(3));
            ByteBuffer buf = ByteBuffer.wrap(payload);

            byte ver = buf.get();
            if (ver != VERSION_V1) {
                throw new IllegalArgumentException("Unsupported version: " + ver);
            }

            byte[] nonce = new byte[NONCE_LEN];
            buf.get(nonce);

            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LEN_BITS, nonce));

            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // GCM은 키/nonce/데이터가 조금만 달라도 여기서 실패(무결성 보호)
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }
}