package com.prafta.web.tbm.tbm01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduInfoRequest;

public record TbmEduInfoParam(
	String mtrlCd
	, String title
	, String contents
	, String mtrlType
	, String useYn
	, List<TbmEduItemInfoModel> tbmEduItemInfoModelList
	, String gvCmpnyCd
	, String gvUserCd
) {
	public static TbmEduInfoParam from(TbmEduInfoRequest request, TokenInfo tokenInfo) {
		
		if(request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		
		return new TbmEduInfoParam(
			request.getMtrlCd()
			, request.getTitle()
			, request.getContents()
			, request.getMtrlType()
			, request.getUseYn()
			, request.getTbmEduItemInfoModelList()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}
}
