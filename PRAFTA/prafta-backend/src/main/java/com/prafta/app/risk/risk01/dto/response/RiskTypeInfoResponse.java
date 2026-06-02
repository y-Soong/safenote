package com.prafta.app.risk.risk01.dto.response;

import java.util.List;

import com.prafta.app.risk.risk01.result.RiskCategoryResult;
import com.prafta.app.risk.risk01.result.RiskHazardResult;
import com.prafta.app.risk.risk01.result.RiskTypeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-036-B2: 위험성평가 구분/분류/발생상황 조회 응답.
 *
 * <p>응답 키 보존(앱 FE Risk_01.vue L576-580 의존):
 *   <ul>
 *     <li>riskCategoryList</li>
 *     <li>riskTypeList</li>
 *     <li>riskHazardList</li>
 *   </ul>
 * <p>D-R2 (NPE 해소): 호출부에서 단일 빌더로 생성하므로 toBuilder 제거.
 *   결과셋이 비어있는 경우에도 List 가 null 이 되지 않도록 service 단에서 빈 List 로 정규화한다.
 */
@Getter
@Builder
public class RiskTypeInfoResponse {
    private final List<RiskCategoryResult> riskCategoryList;
    private final List<RiskTypeResult> riskTypeList;
    private final List<RiskHazardResult> riskHazardList;
}
