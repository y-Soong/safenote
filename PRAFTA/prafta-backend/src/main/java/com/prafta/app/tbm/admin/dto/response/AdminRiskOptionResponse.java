package com.prafta.app.tbm.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * T-K 위험성평가 선택 옵션 응답.
 *
 * <p>표시명(displayName)은 Risk03 규약(공정명/위험요인구분명/유해요인명)을 합성한다.
 */
@Getter
@Builder
public class AdminRiskOptionResponse {
    private List<RiskOptionItem> riskList;

    @Getter
    @Builder
    public static class RiskOptionItem {
        private String siteCd;
        private String processCd;
        private String processNm;
        private String riskTypeCd;
        private String riskTypeNm;
        private String hazardCd;
        private String hazardNm;
        private String assessmentCd;
        private String assessmentStatus;
        private String assessmentStatusNm;
        private String displayName;
    }
}
