package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 연계 위험성평가(A7) 조회 결과.
 *
 * <p>web Tbm02Mapper.selectSessionRisks 4필드(공정/위험요인유형/유해위험요인/평가상태) 포팅.
 * <p>displayName(공정 · 유형 · 유해위험요인)은 서비스에서 합성한다.
 */
@Getter
@Setter
public class TbmSessionRiskResult {
    private String processNm;
    private String riskTypeNm;
    private String hazardNm;
    private String assessmentStatus;
    private String assessmentStatusNm;
    private Integer displayOrder;
}
