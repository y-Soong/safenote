package com.prafta.web.user.user08.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 서명 이력 목록 응답 (GET /webApi/user08/contract-sign-lists).
 */
@Value
@Builder
public class ContractSignListResponse {
    List<ContractSignItem> contractSignList;
    int totalCount;
}
