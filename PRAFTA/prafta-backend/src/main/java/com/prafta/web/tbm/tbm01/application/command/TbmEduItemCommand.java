package com.prafta.web.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduItemModel;

public record TbmEduItemCommand(
		String mtrlItemCd
		// 삭제 시 마스터 회사 스코프 가드용(IDOR 방어)
		, String gvCmpnyCd
){
	public static TbmEduItemCommand from(TbmEduItemModel model, String gvCmpnyCd) {

		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new TbmEduItemCommand(
			model.mtrlItemCd()
			, gvCmpnyCd
		);
	}
}
