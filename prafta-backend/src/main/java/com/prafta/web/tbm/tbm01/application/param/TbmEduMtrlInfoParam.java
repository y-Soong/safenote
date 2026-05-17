package com.prafta.web.tbm.tbm01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduMtrlModel;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduMtrlInfoRequest;

public record TbmEduMtrlInfoParam(
	List<TbmEduMtrlModel> tbmEduMtrlModelList
){
	public static TbmEduMtrlInfoParam from(List<TbmEduMtrlInfoRequest> requests, TokenInfo tokenInfo) {
		
		// 1) 리스트 자체 검증
        if (requests == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);
        
        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<TbmEduMtrlModel> models = requests.stream()
            .map(req -> {
                return new TbmEduMtrlModel(
                    req.getMtrlCd()
                    , req.getTitle()
                    , req.getContents()
                    , req.getMtrlType()
                    , req.getUseYn()
                    , tokenInfo.gv_cmpnyCd()
                    , tokenInfo.gv_userCd()
                );
            })
            .toList();

        return new TbmEduMtrlInfoParam(models);
	}
}
