package com.prafta.common.security.normalize;

public final class Normalizers {
    private Normalizers(){}

    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        // 숫자만 남김
        String digits = raw.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    public static String normalizeEmail(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase();
        return v.isBlank() ? null : v;
    }

    public static String normalizeBirth(String raw) {
        if (raw == null) return null;
        // 숫자만: YYYYMMDD 형태를 기대
        String digits = raw.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    public static String last4(String digits) {
        if (digits == null || digits.length() < 4) return null;
        return digits.substring(digits.length() - 4);
    }

    public static String emailDomain(String email) {
        if (email == null) return null;
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return null;
        return email.substring(at + 1);
    }
}