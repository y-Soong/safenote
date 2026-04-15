package com.prafta.web.baim.baim02.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim02.application.model.CompCmmCodeDModel;
import com.prafta.web.baim.baim02.dto.request.CompCmmCodeDRequest;


public record CompCmmCodeDParam(
	List<CompCmmCodeDModel> compCmmCodeDModelList	
){
	public static CompCmmCodeDParam from(List<CompCmmCodeDRequest> requests, TokenInfo tokenInfo) {

        // 1) 리스트 자체 검증
        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - CompCmmCodeDRequest");
        
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");

        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<CompCmmCodeDModel> models = requests.stream()
            .map(req -> {
                return new CompCmmCodeDModel(
                    req.getChk()
                    , req.getCmpnyCd()
                    , req.getBaimValCd()
                    , req.getBaimValDCd()
                    , req.getBaimValDNm()
                    , req.getSortIdx()
                    , req.getUseYn()
                    , req.getValDInfo1()
                    , req.getValDInfo2()
                    , req.getValDDesc()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new CompCmmCodeDParam(models);
    }
}
