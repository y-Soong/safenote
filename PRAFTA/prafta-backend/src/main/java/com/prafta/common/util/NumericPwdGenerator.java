package com.prafta.common.util;

import java.security.SecureRandom;

/**
 * 숫자 비밀번호(랜덤 6자리 등) 생성 유틸리티.
 *
 * <p>도메인 비종속(숫자 난수 생성)이며 보안 민감도가 낮은 표시용 코드 생성이라
 * {@code common.util}에 위치한다. 평문 보관용이 아닌 일회성 입실/종료 코드 등에 사용한다.
 *
 * <p>최초 사용처: TBM 세션 입실/종료 비밀번호(prafta-033-B). {@link #generatePair(int)}로
 * 입실≠종료를 보장하는 한 쌍을 생성한다.
 */
public final class NumericPwdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private NumericPwdGenerator() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 지정한 자리수의 숫자 비밀번호를 생성한다. 앞자리 0을 허용한다(zero-padding).
     *
     * @param length 자리수(1 이상)
     * @return 길이 {@code length}의 숫자 문자열
     */
    public static String generate(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be >= 1");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 서로 다른 두 개의 숫자 비밀번호 쌍을 생성한다(예: 입실/종료 비밀번호).
     * 첫 번째와 두 번째 값이 동일하지 않음을 보장한다.
     *
     * @param length 각 비밀번호의 자리수
     * @return 길이 2의 배열 {@code [first, second]} (first != second)
     */
    public static String[] generatePair(int length) {
        String first = generate(length);
        String second;
        do {
            second = generate(length);
        } while (second.equals(first));
        return new String[] { first, second };
    }
}
