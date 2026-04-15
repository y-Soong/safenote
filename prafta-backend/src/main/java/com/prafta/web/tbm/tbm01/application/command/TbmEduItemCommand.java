package com.prafta.web.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;

public record TbmEduItemCommand(
		String mtrlItemCd
){
	public static TbmEduItemCommand from(TbmEduItemModel model) {
		
		if(model == null)
			throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n필수값 누락 - TbmEduItemModel");
		
		return new TbmEduItemCommand(
			model.mtrlItemCd()
		);
	}
}
