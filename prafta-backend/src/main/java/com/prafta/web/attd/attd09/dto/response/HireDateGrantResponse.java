package com.prafta.web.attd.attd09.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 입사일 기준 연차 부여 응답 (테스트/검증용).
 * POST /attd09/leave-grant/hire-date-grant.
 */
@Value
@Builder
public class HireDateGrantResponse {

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
