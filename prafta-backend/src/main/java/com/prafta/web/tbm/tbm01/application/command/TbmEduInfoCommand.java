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
	, String siteCd			// prafta-033-A: 스코프(NULL=회사공통)
	, String gvCmpnyCd
	, String gvUserCd
){
	public static TbmEduInfoCommand from(TbmEduInfoParam param, String mtrlCd) {

		if(param == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if(mtrlCd == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 빈 문자열은 회사공통(NULL)로 정규화
		String siteCd = (param.siteCd() == null || param.siteCd().isEmpty()) ? null : param.siteCd();

		return new TbmEduInfoCommand(
			mtrlCd
			, param.title()
			, param.contents()
			, param.mtrlType()
			, param.useYn()
			, siteCd
			, param.gvCmpnyCd()
			, param.gvUserCd()
		);
	}

	public static TbmEduInfoCommand from(TbmEduMtrlModel model) {

		if(model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 목록 그리드 인라인 저장 경로. 스코프(SITE_CD)는 그리드에서 변경하지 않으므로
		// 모델 값을 그대로 사용(빈 값=회사공통). 매퍼에서 IFNULL 처리로 기존 값 보존.
		String siteCd = (model.siteCd() == null || model.siteCd().isEmpty()) ? null : model.siteCd();

		return new TbmEduInfoCommand(
			model.mtrlCd()
			, model.title()
			, model.contents()
			, model.mtrlType()
			, model.useYn()
			, siteCd
			, model.gvCmpnyCd()
			, model.gvUserCd()
		);
	}
}
