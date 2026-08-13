package com.prafta.platform.customer.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 회사 통상근로시간 기준값 변경 응답 (POST /platformApi/customer/std-work-policy).
 *
 * <p>화면이 저장 직후 상태를 그대로 반영할 수 있도록 저장 결과를 되돌려준다.
 */
@Value
@Builder
public class StdWorkPolicyUpdateResponse {

    /** 대상 회사코드. */
    String cmpnyCd;

    /** 저장된 주 소정근로 분. 지정 해제했으면 null(= 코드 폴백 2400분 적용). */
    Integer weekStdMinutes;

    /** 'Y'=직접 지정 행 존재 / 'N'=미지정(기본 주 40시간). */
    String policyCustomYn;
}
