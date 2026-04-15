package com.prafta.common.cmm.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.auth.result.AuthTokenResult;
import com.prafta.common.cmm.login.result.UserResult;

@Mapper
public interface AuthMapper {
	AuthTokenResult selectValidByRefreshTokenHash(@Param("refreshTokenHash") String refreshToken);
	
//	AuthToken selectByRefreshTokenHash(@Param("hash") String hash);
	
	UserResult selectUserForJwt(@Param("userCd") String hash);
}
