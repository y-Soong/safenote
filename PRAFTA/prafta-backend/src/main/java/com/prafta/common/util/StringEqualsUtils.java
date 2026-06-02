package com.prafta.common.util;

/**
 * null과 빈 문자열을 동치로 보는 문자열 비교 헬퍼.
 *
 * <p>주로 "요청 본문이 권위 있는 DB 행(row)과 일치하는지" 검사에 사용된다.
 * 옵셔널 컬럼(예: NODE_CD)은 한쪽은 null, 다른 쪽은 ""인 경우가 정상적으로
 * 발생할 수 있는데, 이런 사소한 차이를 변조(tampering)로 탐지하면 안 된다.
 *
 * <p>원래 {@code Attd07ServiceImpl}에서 추출됨 (PRAFTA-003-1).
 */
public final class StringEqualsUtils {

    private StringEqualsUtils() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * 두 값이 다르면 true를 반환한다.
     * null과 빈 문자열은 동치로 취급한다.
     */
    public static boolean isMismatched(String a, String b) {
        String left  = a == null ? "" : a;
        String right = b == null ? "" : b;
        return !left.equals(right);
    }
}
