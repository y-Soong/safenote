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
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduItemInfoModel");
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduInfoParam");
		if(mtrlItemCd == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mtrlItemCd");
		if(mtrlCd == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mtrlCd");
		
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
