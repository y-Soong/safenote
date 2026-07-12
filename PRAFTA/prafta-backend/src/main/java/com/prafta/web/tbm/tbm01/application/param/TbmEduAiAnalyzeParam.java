package com.prafta.web.tbm.tbm01.application.param;

import java.util.List;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm01.application.model.TbmEduAiAnalyzeItemModel;
import com.prafta.web.tbm.tbm01.dto.request.TbmEduAiAnalyzeRequest;

/**
 * TBM 교육자료 AI 분석 지정 저장 파라미터.
 * <p>잠긴(사용 중) 교육자료에 대해서도 AI_ANALYZE_YN 만 갱신하는 전용 경로.
 * 권한/회사 스코프(IDOR) 검증은 서비스에서 수행하므로 gv 클레임을 함께 보관한다.
 */
public record TbmEduAiAnalyzeParam(
	String mtrlCd
	, List<TbmEduAiAnalyzeItemModel> itemList
	, String gvCmpnyCd
	, String gvAuthCd
	, String gvUserCd
){
	public static TbmEduAiAnalyzeParam from(TbmEduAiAnalyzeRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		List<TbmEduAiAnalyzeItemModel> models = (request.getItemList() == null)
			? List.of()
			: request.getItemList().stream()
				.filter(req -> req != null)
				.map(req -> new TbmEduAiAnalyzeItemModel(
					req.getMtrlItemCd()
					, req.getAiAnalyzeYn()
				))
				.toList();

		return new TbmEduAiAnalyzeParam(
			request.getMtrlCd()
			, models
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
