package com.prafta.web.chkLst.chkLst05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.chkLst.chkLst05.dto.request.HistListRequest;

/**
 * 순회점검 이력 조회 파라미터(PRAFTA-SUBCON-T6-AUDIT-03).
 *
 * <p>테넌트/사용자 스코프(gvCmpnyCd/gvUserCd)는 서버 세션 클레임에서만 취한다(요청 파라미터 불신 — IDOR 차단).
 * 사업장·기간은 필수(전 사업장 스캔 금지 — 감사 정밀성 + 성능).
 */
public record HistListParam(
	String siteCd
	, String fromWorkDate
	, String toWorkDate
	, String chkptCd
	, String inspectItemCd
	, String gvCmpnyCd
	, String gvUserCd
){
	public static HistListParam from(HistListRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		// 사업장·기간 필수(미지정 조회 = 전 사업장 스캔 차단).
		if (isEmpty(request.getSiteCd())
				|| isEmpty(request.getFromWorkDate())
				|| isEmpty(request.getToWorkDate()))
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new HistListParam(
			request.getSiteCd()
			, request.getFromWorkDate()
			, request.getToWorkDate()
			, request.getChkptCd()
			, request.getInspectItemCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_userCd()
		);
	}

	private static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}
}
