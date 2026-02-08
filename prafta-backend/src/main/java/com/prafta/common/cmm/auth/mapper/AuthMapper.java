package com.prafta.common.cmm.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.auth.vo.AuthToken;
import com.prafta.common.cmm.auth.vo.UserInfo;

@Mapper
public interface AuthMapper {
	AuthToken selectValidByRefreshTokenHash(@Param("refreshTokenHash") String refreshToken);
	
//	AuthToken selectByRefreshTokenHash(@Param("hash") String hash);
	
	UserInfo selectUserForJwt(@Param("userId") String hash);
}
