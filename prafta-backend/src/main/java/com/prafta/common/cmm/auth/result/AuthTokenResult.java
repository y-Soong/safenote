package com.prafta.common.cmm.auth.result;

import java.util.Date;

public record AuthTokenResult (
	String cmpnyCd
	, String userCd
    , String userId
    , String tokenId

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
