package com.prafta.web.risk.risk01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk01.application.model.RiskTypeModel;
import com.prafta.web.risk.risk01.dto.request.RiskTypeRequest;


public record RiskTypeParam(
	List<RiskTypeModel> riskTypeModelList
){
	public static RiskTypeParam from(List<RiskTypeRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<RiskTypeModel> models = requests.stream()
            .map(req -> {
                return new RiskTypeModel(
                    req.getCmpnyCd()
                    , req.getProcessCd()
                    , req.getRiskTypeCd()
                    , req.getRiskTypeNm()
                    , req.getSiteCd()
                    , req.getUseYn()
                    , req.getRiskTypeDesc()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new RiskTypeParam(models);
    }
}
