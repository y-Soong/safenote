package com.prafta.web.tbm.tbm01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduItemRequest;

public record TbmEduItemParam(
	List<TbmEduItemModel> tbmEduItemModelList
){
	public static TbmEduItemParam from(List<TbmEduItemRequest> requests, TokenInfo tokenInfo) {
		
		// 1) 리스트 자체 검증
        if (requests == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduItemRequest");
        if (tokenInfo == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TokenInfo");
        
        // 2) null element 방지 + 3) 필수값 검증 + 4) 매핑
        List<TbmEduItemModel> models = requests.stream()
            .map(req -> {
                return new TbmEduItemModel(
                    req.getMtrlItemCd()
                );
            })
            .toList();

        return new TbmEduItemParam(models);
	}
}
