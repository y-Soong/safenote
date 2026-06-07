package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 위험성평가 3계층 드롭다운 옵션 요청 (공정 → 위험요인구분 → 유해요인).
 * 상위 계층 선택값으로 하위 옵션을 좁힌다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RiskCategoryOptionRequest {
    private String siteCd;
    private String processCd;   // 위험요인구분 옵션 좁히기
    private String riskTypeCd;  // 유해요인 옵션 좁히기
}
