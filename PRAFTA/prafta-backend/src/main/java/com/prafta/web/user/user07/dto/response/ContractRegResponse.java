package com.prafta.web.user.user07.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 등록/교체 응답 (POST /webApi/user07/contract).
 * 업로드 = 새 버전 생성(서명자 전원 재서명 트리거 — D8-②).
 */
@Value
@Builder
public class ContractRegResponse {
    int contractVer;
}
