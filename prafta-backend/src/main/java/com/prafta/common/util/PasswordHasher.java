package com.prafta.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

@Component
public class PasswordHasher {

    private final PasswordEncoder encoder;
    private final String pepper; // optional

    public PasswordHasher(
            @Value("${security.password.bcrypt.strength:12}") int strength,
            @Value("${security.password.pepper:}") String pepper
    ) {
        this.encoder = new BCryptPasswordEncoder(strength);
        this.pepper = pepper == null ? "" : pepper;
    }

    public String hash(String plainPassword) {
        validate(plainPassword);
        return encoder.encode(applyPepper(plainPassword));
    }

    public boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return encoder.matches(applyPepper(plainPassword), storedHash);
    }

    private String applyPepper(String pw) {
        return pepper.isEmpty() ? pw : (pw + ":" + pepper);
    }

    private void validate(String pw) {
        if (pw == null || pw.isBlank()) throw new IllegalArgumentException("password is required");
        if (pw.length() > 128) throw new IllegalArgumentException("password too long");
    }
}