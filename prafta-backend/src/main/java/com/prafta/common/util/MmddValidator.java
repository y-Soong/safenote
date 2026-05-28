package com.prafta.common.util;

import java.util.regex.Pattern;

/**
 * MMDD(월일 4자리) 형식 유효성 검증 유틸리티.
 *
 * <p>주로 연차 타입 자동부여 규칙 중 "부여일지정"(SYS027 = '03') 케이스에서
 * 사용되는 {@code GRANT_ASSIGN_MMDD} 컬럼 입력값 검증에 사용된다.
 *
 * <p>검증 규칙(정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.2):
 * <ul>
 *   <li>형식: {@code ^(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$}</li>
 *   <li>월별 일수 상한:
 *     <ul>
 *       <li>02월 → 29일(02/29 허용. 평년 02/28 fallback은 부여 실행 시점 책임)</li>
 *       <li>04/06/09/11월 → 30일</li>
 *       <li>그 외 → 31일</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>PRAFTA-017로 도입.
 */
public final class MmddValidator {

    /** 1차 정규식 검증 패턴 (월=01~12, 일=01~31). */
    private static final Pattern MMDD_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$");

    private MmddValidator() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /**
     * MMDD 4자리 문자열이 형식과 월별 일수 규칙을 모두 만족하는지 검사한다.
     *
     * @param mmdd 검증 대상 (예: "0301", "0229")
     * @return 유효하면 true, null/공백/형식 위반/일수 초과 시 false
     */
    public static boolean isValid(String mmdd) {
        if (mmdd == null || mmdd.isBlank()) {
            return false;
        }
        // 1차: 정규식 검증
        if (!MMDD_PATTERN.matcher(mmdd).matches()) {
            return false;
        }
        // 2차: 월별 일수 상한 검증
        int month = Integer.parseInt(mmdd.substring(0, 2));
        int day = Integer.parseInt(mmdd.substring(2, 4));

        int maxDay;
        switch (month) {
            case 2:
                // 02/29 허용 (평년 02/28 fallback은 부여 실행 스케줄러 책임)
                maxDay = 29;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                maxDay = 30;
                break;
            default:
                maxDay = 31;
                break;
        }
        return day <= maxDay;
    }
}
