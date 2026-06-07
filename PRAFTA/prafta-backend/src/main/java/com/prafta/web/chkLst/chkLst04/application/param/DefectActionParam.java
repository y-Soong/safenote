package com.prafta.web.chkLst.chkLst04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst04.dto.request.DefectActionRequest;

public record DefectActionParam(
	String siteCd
	, String chkptCd
	, String inspectItemCd
	, String workDate
	, String actionDesc
	, String gvCmpnyCd
	, String gvUserCd
){
	// 조치 상세 최대 길이(DB TEXT 컬럼이라 무제한 입력 방어)
	private static final int MAX_ACTION_DESC_LEN = 4000;

	public static DefectActionParam from(DefectActionRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 불량 식별키 필수값 검증(IDOR/오저장 방지)
		if (isEmpty(request.getSiteCd())
				|| isEmpty(request.getChkptCd())
				|| isEmpty(request.getInspectItemCd())
				|| isEmpty(request.getWorkDate()))
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 조치 상세 필수
		if (isEmpty(request.getActionDesc()))
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 조치 상세 길이 상한(DB는 TEXT, 서버측 합리적 상한 4000자)
		if (request.getActionDesc().length() > MAX_ACTION_DESC_LEN)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new DefectActionParam(
			request.getSiteCd()
			, request.getChkptCd()
			, request.getInspectItemCd()
			, request.getWorkDate()
			, request.getActionDesc()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}

	private static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}
}
