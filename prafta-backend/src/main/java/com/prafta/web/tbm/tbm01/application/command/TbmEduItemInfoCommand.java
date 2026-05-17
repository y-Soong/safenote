package com.prafta.web.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemInfoModel;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;

public record TbmEduItemInfoCommand(
		String mtrlItemCd
		, String mtrlCd
		, String mtrlItemType
		, String sortIdx
		, String fileMgmtCd
		, String mtrlDesc
		, String url
		, String useYn
		, String gvCmpnyCd
		, String gvUserCd
){
	public static TbmEduItemInfoCommand from(TbmEduItemInfoModel model, TbmEduInfoParam param, String mtrlItemCd, String mtrlCd, String fileMgmtCd) {
		
		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mtrlItemCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mtrlCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		
		return new TbmEduItemInfoCommand(
			mtrlItemCd
			, mtrlCd
			, model.mtrlItemType()
			, model.sortIdx()
			, fileMgmtCd
			, model.mtrlDesc()
			, model.url()
			, model.useYn()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
