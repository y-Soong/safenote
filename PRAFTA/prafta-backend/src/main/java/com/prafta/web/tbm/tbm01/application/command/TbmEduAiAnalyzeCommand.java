package com.prafta.web.tbm.tbm01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduAiAnalyzeItemModel;

/**
 * TBM 교육자료 AI 분석 지정 갱신 커맨드(항목 단위).
 * <p>gvCmpnyCd 는 UPDATE WHERE 절 회사 스코프 가드(IDOR 방어)에 사용.
 */
public record TbmEduAiAnalyzeCommand(
	String mtrlItemCd
	, String aiAnalyzeYn
	, String gvUserCd
	, String gvCmpnyCd
){
	public static TbmEduAiAnalyzeCommand from(TbmEduAiAnalyzeItemModel model, String gvUserCd, String gvCmpnyCd) {

		if (model == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new TbmEduAiAnalyzeCommand(
			model.mtrlItemCd()
			, model.aiAnalyzeYn()
			, gvUserCd
			, gvCmpnyCd
		);
	}
}
