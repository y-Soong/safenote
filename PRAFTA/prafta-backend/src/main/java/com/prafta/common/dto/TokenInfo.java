package com.prafta.common.dto;

import java.util.Date;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import io.jsonwebtoken.Claims;

/**
 * JWT 클레임에서 추출한 사용자 식별·인가 정보.
 * 정책 §11.1(최소 수집·목적 제한)에 따라 휴대폰(gv_mblNo) / 이메일(gv_email) 등 PII는
 * JWT 클레임 및 본 레코드에서 제외한다.
 */
public record TokenInfo (
	String gv_cmpnyCd
	, String gv_userCd
	, String gv_userId
	, String gv_userNm
	, String gv_authCd
	, String gv_authLevel
	, String gv_siteCd
	, String gv_siteNo
	, String gv_siteNm
	, String gv_nodeCd
	, String gv_nodeNm
	, String gv_deviceId
	// PRAFTA-app-027 follow-up: 고용형태[SYS041] (REGULAR/CONTRACT/DAILY/EXECUTIVE).
	//   통합형 일용직 차단(assertNotDailyWorker)의 단일 신뢰 출처. 정규 사용자엔 NULL/비-DAILY.
	, String gv_employmentType
	, Date issuedAt
	, Date getExpiration
) {
	public static TokenInfo from(Claims claims) {

		if (claims == null)
        	throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TokenInfo(
        		claims.get("gv_cmpnyCd", String.class)
        		, claims.get("gv_userCd", String.class)
        		, claims.get("gv_userId", String.class)
        		, claims.get("gv_userNm", String.class)
        		, claims.get("gv_authCd", String.class)
        		, claims.get("gv_authLevel", String.class)
        		, claims.get("gv_siteCd", String.class)
        		, claims.get("gv_siteNo", String.class)
        		, claims.get("gv_siteNm", String.class)
        		, claims.get("gv_nodeCd", String.class)
        		, claims.get("gv_nodeNm", String.class)
        		, claims.get("gv_deviceId", String.class)
        		, claims.get("gv_employmentType", String.class)
        		, claims.getIssuedAt()
        		, claims.getExpiration()
        );
    }
}
