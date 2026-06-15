package com.prafta.web.risk.risklink01.dto.response;

import java.util.List;

import com.prafta.web.risk.risklink01.result.LinkNearMissResult;

import lombok.Builder;
import lombok.Getter;

/**
 * L1 연결 후보 목록 / L2 연결됨 목록 공통 응답.
 */
@Getter
@Builder
public class NearMissLinkListResponse {
    private List<LinkNearMissResult> nearMissList;
}
