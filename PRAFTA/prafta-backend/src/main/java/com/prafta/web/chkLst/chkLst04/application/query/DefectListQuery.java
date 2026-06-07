package com.prafta.web.chkLst.chkLst04.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.application.param.DefectListParam;

public record DefectListQuery(
	String siteCd
	, String chkLstType
	, String chkptCd
	, String inspectItemCd
	, String actionStatus
	, String fileType			// 첨부사진 FILE_TYPE(점검=SYS010 '001')
	, String gvCmpnyCd
	, String gvUserCd
){
	// 점검(일일점검) 첨부사진 파일타입(SYS010 '001'). chkLst03 InspectAnswerQuery 와 동일 관례.
	private static final String FILE_TYPE_INSPECT = "001";

	public static DefectListQuery from(DefectListParam param) {

		if (param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new DefectListQuery(
			param.siteCd()
			, param.chkLstType()
			, param.chkptCd()
			, param.inspectItemCd()
			, param.actionStatus()
			, FILE_TYPE_INSPECT
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
