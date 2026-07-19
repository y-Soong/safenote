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
	, String fileType			// 점검 첨부사진 FILE_TYPE(점검=SYS010 '001')
	, String actionFileType		// 조치 첨부사진 FILE_TYPE(조치=SYS010 '006')
	, String gvCmpnyCd
	, String gvUserCd
){
	// 점검(일일점검) 첨부사진 파일타입(SYS010 '001'). chkLst03 InspectAnswerQuery 와 동일 관례.
	private static final String FILE_TYPE_INSPECT = "001";

	// [정책변경 §2] 조치 첨부사진 파일타입(SYS010 '006' 점검조치사진).
	private static final String FILE_TYPE_DEFECT_ACTION = "006";

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
			, FILE_TYPE_DEFECT_ACTION
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}
}
