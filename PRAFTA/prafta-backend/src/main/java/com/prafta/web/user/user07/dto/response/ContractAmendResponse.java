package com.prafta.web.user.user07.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 in-place 정정 응답 (POST /webApi/user07/contract-amend — 승인시점 버전확정 T3).
 *
 * <p>정정은 <b>버전을 올리지 않는다</b>(J9). 따라서 반환되는 {@code contractVer} 는 정정 대상 버전
 * 그대로이며, 재서명 트리거도 발생하지 않는다(등록/교체 응답과 의미가 다르다).
 */
@Value
@Builder
public class ContractAmendResponse {
    int contractVer;
}
