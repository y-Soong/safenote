package com.prafta.web.attd.attd09.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 정책 기준 부여 적용 응답 (prafta-022 작업 D).
 * POST /attd09/leave-grant/policy-grant.
 *
 * <p>{@code HireDateGrantResponse}와 동일한 부여 결과(신규 부여/변경 없음)를 반환한다.
 *
 * <p>prafta-032 D6: 입사일 변경 처리방식 자동계산(RESET_ALL) 폐기로
 * "취소(재발급) 건수(canceledCount)"를 제거했다(런타임상 항상 0).
 */
@Value
@Builder
public class PolicyGrantResponse {

    /** 신규 부여가 발생한 직원 수 */
    int grantedCount;

    /** 신규 부여된 사용자 코드 목록 */
    List<String> grantedUserCds;

    /** 건너뛴(이미 부여됨/부여 대상 아님) 직원 수 */
    int skippedCount;

    /** 건너뛴 사용자 코드 목록 */
    List<String> skippedUserCds;

    /** 이번 호출로 신규 부여된 총 일수 */
    BigDecimal grantedDays;
}
