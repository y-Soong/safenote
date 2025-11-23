package com.prafta.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHashing {

    // 비용(cost) 값. 10~12 권장(높을수록 느리고 안전). 환경에 맞춰 조정.
    private static final int BCRYPT_STRENGTH = 12;
    private static final PasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    // 비밀번호 해시 생성 (회원가입 시)
    public static String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    // 비밀번호 검증 (로그인 시)
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        return encoder.matches(plainPassword, storedHash);
    }	
}
