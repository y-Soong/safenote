package com.prafta.common.dto;

import java.util.Date;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import io.jsonwebtoken.Claims;

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
	, String gv_mblNo
	, String gv_email
	, String gv_deviceId
	, Date issuedAt
	, Date getExpiration
) {
	public static TokenInfo from(Claims claims) {
		
		if (claims == null)
        	throw ApiException.appendf(CommonErrorCode.COMMON_400_001,"\n�ʼ��� ���� - Claims");
		
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
        		, claims.get("gb_nodeCd", String.class)
        		, claims.get("gv_nodeNm", String.class)
        		, claims.get("gv_mblNo", String.class)
        		, claims.get("gv_email", String.class)
        		, claims.get("gv_deviceId", String.class)
        		, claims.getIssuedAt()
        		, claims.getExpiration()
        );
    }
}
