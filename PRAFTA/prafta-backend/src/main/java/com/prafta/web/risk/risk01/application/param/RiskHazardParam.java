package com.prafta.web.risk.risk01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.model.RiskHazardModel;
import com.prafta.web.risk.risk01.dto.request.RiskHazardRequest;

public record RiskHazardParam(
	List<RiskHazardModel> riskHazardModelList
){
    public static RiskHazardParam from(List<RiskHazardRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<RiskHazardModel> models = requests.stream()
            .map(req -> {
                return new RiskHazardModel(
                    req.getCmpnyCd()
                    , req.getRiskTypeCd()
                    , req.getHazardCd()
                    , req.getHazardNm()
                    , req.getSiteCd()
                    , req.getHazardDesc()                   
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new RiskHazardParam(models);
    }
}
