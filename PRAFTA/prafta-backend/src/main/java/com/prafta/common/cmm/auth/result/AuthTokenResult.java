package com.prafta.common.cmm.auth.result;

import java.util.Date;

public record AuthTokenResult (
	String cmpnyCd
	, String userCd
    , String tokenId
    , String loginId          // prafta-057: 로그인 세션 패밀리 식별자(회전 시 승계)

    , String clientType
    , String deviceId
 
    , String refreshTokenHash

    , Date issuedDtime
    , Date expireDtime

    , String revokedYn
    , Date revokedDtime

    , String ipAddr
    , String userAgent

    , String insertNo
    , Date insertDate
    , String updateNo
    , Date updateDate
) {
	
}
