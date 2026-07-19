package com.prafta.web.user.user07.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 관리 조회 응답 (GET /webApi/user07/contract-lists — UI-DC-05).
 * activeContract 는 활성 계약서 요약 카드용(없으면 null), versionList 는 버전 이력 테이블용.
 */
@Value
@Builder
public class ContractListResponse {
    ContractVersionItem activeContract;
    List<ContractVersionItem> versionList;
    int totalCount;
}
