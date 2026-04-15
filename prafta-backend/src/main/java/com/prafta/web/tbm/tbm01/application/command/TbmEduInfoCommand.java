package com.prafta.web.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduMtrlModel;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduMtrlInfoParam;

public record TbmEduInfoCommand(
	String mtrlCd
	, String title
	, String contents
	, String mtrlType
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
){
	public static TbmEduInfoCommand from(TbmEduInfoParam param, String mtrlCd) {
		
		if(param == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduInfoParam");
		if(mtrlCd == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - mtrlCd");
		
		return new TbmEduInfoCommand(
			mtrlCd
			, param.title()
			, param.contents()
			, param.mtrlType()
			, param.useYn()
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
	
	public static TbmEduInfoCommand from(TbmEduMtrlModel model) {
		
		if(model == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduMtrlModel");
		
		return new TbmEduInfoCommand(
			model.mtrlCd()
			, model.title()
			, model.contents()
			, model.mtrlType()
			, model.useYn()
			, model.gvCmpnyCd()
			, model.gvUserCd()
		);
	}
}
