package com.prafta.web.tbm.tbm04.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * W-13 확장(2026-08-30) — 출결 서명 이미지 스트림 파라미터.
 *
 * <p>클라는 attendanceCd + kind(ENTRY|EXIT)만 보내고, 파일 식별자(FILE_MGMT_CD)는
 * 서버가 출결 행에서 재조회한다(클라 파일코드 신뢰 금지 — IDOR 방지).
 */
public record AttendanceSignImageParam(
	String attendanceCd
	, String kind			// ENTRY(입실 서명) | EXIT(종료 서명)
	, String gvCmpnyCd
	, String gvSiteCd
	, String gvAuthCd
	, String gvUserCd
){
	public static AttendanceSignImageParam from(String attendanceCd, String kind, TokenInfo tokenInfo) {

		if (tokenInfo == null)
			throw new ApiException(CommonErrorCode.COMMON_400_003);
		if (attendanceCd == null || attendanceCd.isBlank() || attendanceCd.length() > 50)
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		if (!"ENTRY".equals(kind) && !"EXIT".equals(kind))
			throw new ApiException(CommonErrorCode.COMMON_400_001);

		return new AttendanceSignImageParam(
			attendanceCd
			, kind
			, tokenInfo.gv_cmpnyCd()
			, tokenInfo.gv_siteCd()
			, tokenInfo.gv_authCd()
			, tokenInfo.gv_userCd()
		);
	}
}
