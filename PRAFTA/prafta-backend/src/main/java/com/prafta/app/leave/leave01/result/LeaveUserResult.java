package com.prafta.app.leave.leave01.result;

/**
 * prafta-app-005: 사용자 메타 조회 결과(tb_user + 경력인정 합계).
 * <p>매핑(AppLeave01Mapper.selectUser):
 * <pre>
 *   U.USER_NM                           AS userNm              (평문 PII, 로그 출력 금지)
 *   U.HIRE_DATE                         AS hireDate            (YYYYMMDD 원본, FE 포맷)
 *   SUM(C.CREDIT_MONTHS WHERE USE_YN='Y') AS serviceCreditMonths (없으면 0)
 * </pre>
 * serviceMonths(실근속 개월)는 서버 LocalDate 계산(hireDate~오늘)으로 별도 산출한다.
 */
public record LeaveUserResult(
    String userNm
    , String hireDate
    , int serviceCreditMonths
) {
}
