package com.prafta.app.tbm.tbm01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm-A7: 연계 위험성평가 리스트 응답.
 *
 * <p>web SessionRiskItem 4필드(공정/위험요인유형/유해위험요인/평가상태). displayName 은 서비스 합성.
 */
@Getter
@Builder
public class TbmRiskListResponse {

    private final List<Item> risks;

    @Getter
    @Builder
    public static class Item {
        private final String displayName;          // processNm · riskTypeNm · hazardNm
        private final String processNm;
        private final String riskTypeNm;
        private final String hazardNm;
        private final String assessmentStatus;
        private final String assessmentStatusNm;
        private final Integer displayOrder;
    }
}
