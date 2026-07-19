package com.prafta.web.tbm.tbm02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbm02.dto.request.ManagerEnterRequest;

/**
 * 관리자 직접 입실 파라미터(prafta-051-11).
 *
 * <p>sessionCd/userTypeCd/userCd 는 요청에서, 회사/사업장/권한/처리자 식별자는 JWT 에서만 도출한다.
 */
public record ManagerEnterParam(
	String sessionCd
	// PRAFTA-SUBCON-T5 M1: 후보 목록 행의 불투명 핸들(권장 경로). 있으면 이 값만으로 대상이 확정된다.
	, String entryHandle
	, String userTypeCd
	, String userCd
	// 화면에서 고른 대상 회사(개설사 또는 1차 회사). 서버가 assertTier1Selectable 로 검증한다.
	, String targetCmpnyCd
	// QR 페이로드의 회사코드(힌트 전용 — 체인 범위 안에서 동명 USER_CD 를 가를 때만 사용).
	, String qrCmpnyCd
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static ManagerEnterParam from(ManagerEnterRequest request, TokenInfo tokenInfo) {

		if (request == null)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);

		return new ManagerEnterParam(
			request.getSessionCd()
			, request.getEntryHandle()
			, request.getUserTypeCd()
			, request.getUserCd()
			, request.getTargetCmpnyCd()
			, request.getQrCmpnyCd()
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
