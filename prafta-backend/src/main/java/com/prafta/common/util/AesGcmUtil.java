package com.prafta.common.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AesGcmUtil {
    private static final int AES_KEY_BITS = 256; // 128/192/256
    private static final int GCM_IV_LENGTH = 12; // bytes, 권장
    private static final int GCM_TAG_LENGTH = 128; // bits

    private static final SecureRandom secureRandom = new SecureRandom();

    // 키 생성 (운영에서는 KMS/HSM 사용 권장)
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_BITS);
        return keyGen.generateKey();
    }

    // 키를 Base64 문자열로 저장/복원할 때
    public static String keyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static SecretKey keyFromBase64(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decoded, "AES");
    }
    
    // 암호화: 반환값은 Base64( iv || ciphertext )
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        // 합쳐서 저장: iv + cipherText
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    // 복호화: 입력은 Base64( iv || ciphertext )
    public static String decrypt(String base64IvAndCipherText, SecretKey key) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64IvAndCipherText);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        int ctLen = combined.length - iv.length;
        byte[] cipherText = new byte[ctLen];
        System.arraycopy(combined, iv.length, cipherText, 0, ctLen);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, "UTF-8");
    }
}
