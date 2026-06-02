package com.prafta.common.cmm.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.auth.result.AuthTokenResult;
import com.prafta.common.cmm.login.result.UserResult;

@Mapper
public interface AuthMapper {
	// 유효(미폐기 + 미만료)한 refresh token 1건 조회
	AuthTokenResult selectValidByRefreshTokenHash(@Param("refreshTokenHash") String refreshToken);

	// grace window: 최근 graceSeconds 초 이내에 폐기된 refresh token 1건 조회
	// (탭 회전 직후 다른 탭이 옛 RT 로 refresh 시도하는 경우를 완충)
	AuthTokenResult selectRecentlyRevokedByRefreshTokenHash(
			@Param("refreshTokenHash") String refreshTokenHash
			, @Param("graceSeconds") int graceSeconds);

	//	AuthToken selectByRefreshTokenHash(@Param("hash") String hash);

	UserResult selectUserForJwt(@Param("userCd") String hash);

	// 회전 1단계: 기존 refresh token row 폐기
	int revokeTokenById(
			@Param("cmpnyCd") String cmpnyCd
			, @Param("userCd") String userCd
			, @Param("tokenId") String tokenId);

	// 회전 2단계: 신규 refresh token row insert
	int insertRotatedToken(
			@Param("cmpnyCd") String cmpnyCd
			, @Param("userCd") String userCd
			, @Param("tokenId") String tokenId
			, @Param("clientType") String clientType
			, @Param("deviceId") String deviceId
			, @Param("refreshTokenHash") String refreshTokenHash
			, @Param("expireDtime") java.util.Date expireDtime);
}
