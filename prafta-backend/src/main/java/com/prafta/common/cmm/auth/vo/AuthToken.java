package com.prafta.common.cmm.auth.vo;

import java.util.Date;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthToken {
	String cmpnyCd;
    String userId;
    String tokenId;

    String clientType;
    String deviceId;

    String refreshTokenHash;

    Date issuedDtime;
    Date expireDtime;

    String revokedYn;
    Date revokedDtime;

    String ipAddr;
    String userAgent;

    String insertNo;
    Date insertDate;
    String updateNo;
    Date updateDate;
}
