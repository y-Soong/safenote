package com.prafta.common.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.dto.TokenInfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

//    private final String secret = "cleannote-cleaning-platform-jwt-secret-key-2025";
//    private final long expiration = 1000 * 60 * 60 * 1;
//    private final long expiration = 1000 * 60 * 60 * 2;
//    private final long expiration = 1000 * 5;
	
	private final Key key;
    private final long expiration;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expiration
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

//    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());

    /**
     * JWT 토큰 생성.
     * 정책 §11.1(최소 수집·목적 제한)에 따라 휴대폰/이메일 등 PII는 클레임에 포함하지 않는다.
     * JWT 페이로드는 base64로 누구나 디코딩 가능하므로 식별·인가에 필요한 비-PII 정보만 담는다.
     */
    public String generateToken(UserResult userResult) {

        return Jwts.builder()
        		.claim("gv_cmpnyCd", userResult.cmpnyCd())
        		.claim("gv_userCd", userResult.userCd())
        		.claim("gv_userId", userResult.userId())
                .claim("gv_userNm", userResult.userNm())
                .claim("gv_authCd", userResult.authCd())
                .claim("gv_authLevel", userResult.authLevel())
                .claim("gv_siteCd", userResult.siteCd())
                .claim("gv_siteNo", userResult.siteNo())
                .claim("gv_siteNm", userResult.siteNm())
                .claim("gv_nodeCd", userResult.nodeCd())
                .claim("gv_nodeNm", userResult.nodeNm())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * PRAFTA-037-F8 — 일반화된 임시 scope 토큰 발급.
     *
     * <p>정식 토큰 발급 전/특수 흐름(휴대폰 본인인증/비밀번호 재설정/이메일 인증 등)에서
     * 한정된 endpoint 만 접근 허용하는 임시 JWT 를 발급한다. PII 는 절대 클레임에 포함하지 않는다(§11.1).
     *
     * @param cmpnyCd 회사코드 (식별용)
     * @param userCd  사용자코드 (식별용)
     * @param scope   {@link JwtScope} 상수 (예: {@link JwtScope#PHONE_AUTH})
     * @param ttlMinutes 만료 분(권장 5~10)
     */
    public String generateScopeToken(String cmpnyCd, String userCd, String scope, int ttlMinutes) {
        return generateScopeToken(cmpnyCd, userCd, scope, ttlMinutes, null);
    }

    /**
     * PRAFTA-app-010 — 추가 커스텀 클레임을 받는 scope 토큰 발급 오버로드.
     *
     * <p>휴대폰 변경 검증처럼 "검증한 대상(번호 HMAC)" 을 토큰에 바인딩해야 하는 흐름에서 사용한다.
     * extraClaims 에도 PII 평문은 절대 담지 않는다(§11.1). 휴대폰은 HMAC 등 비가역 식별자만 허용한다.
     * 기존 무인자 호출처는 {@code extraClaims=null} 위임으로 무변경.
     *
     * @param extraClaims 추가 클레임(키는 {@code gv_} 접두 권장). null 이면 추가 클레임 없음.
     */
    public String generateScopeToken(String cmpnyCd, String userCd, String scope, int ttlMinutes,
                                     Map<String, Object> extraClaims) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .claim("gv_cmpnyCd", cmpnyCd)
                .claim("gv_userCd", userCd)
                .claim("gv_scope", scope);
        if (extraClaims != null) {
            for (Map.Entry<String, Object> e : extraClaims.entrySet()) {
                builder.claim(e.getKey(), e.getValue());
            }
        }
        return builder
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMinutes * 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * PRAFTA-036 — 휴대폰 본인인증 전용 임시 토큰 발급(scope=PHONE_AUTH).
     *
     * <p>PRAFTA-037-F8 으로 일반화된 {@link #generateScopeToken} 에 위임한다. 기존 호출처는 무변경.
     */
    public String generatePhoneAuthScopeToken(String cmpnyCd, String userCd, int ttlMinutes) {
        return generateScopeToken(cmpnyCd, userCd, JwtScope.PHONE_AUTH, ttlMinutes);
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getCmpnyCdFromToken(String token) {
        return parseToken(token).get("gv_cmpnyCd", String.class);
    }
    
    public String getUserCdFromToken(String token) {
        return parseToken(token).get("gv_userCd", String.class);
    }
    
    public String getUserIdFromToken(String token) {
        return parseToken(token).get("gv_userId", String.class);
    }

    public String getUserNmFromToken(String token) {
        return parseToken(token).get("gv_userNm", String.class);
    }
    
    public String getAuthCdFromToken(String token) {
        return parseToken(token).get("gv_authCd", String.class);
    }
    
    public String getAuthLevelFromToken(String token) {
        return parseToken(token).get("gv_authLevel", String.class);
    }
    
    public String getSiteCdFromToken(String token) {
        return parseToken(token).get("gv_siteCd", String.class);
    }
    
    public String getSiteNoFromToken(String token) {
        return parseToken(token).get("gv_siteNo", String.class);
    }
    
    public String getSiteNmFromToken(String token) {
        return parseToken(token).get("gv_siteNm", String.class);
    }
    
    public String getNodeCdFromToken(String token) {
        return parseToken(token).get("gv_nodeCd", String.class);
    }
    
    public String getNodeNmFromToken(String token) {
        return parseToken(token).get("gv_nodeNm", String.class);
    }

    // gv_mblNo / gv_email getter는 정책 §11.1에 따라 JWT 클레임에서 제거됨.
    // 휴대폰/이메일이 필요한 화면은 /comApi/baseinfo/user-info-lists API로 조회한다.

    public TokenInfo getAllClaimsAsMap(String authorization) {
    	if (authorization != null && authorization.startsWith("Bearer ")) {
    		
            String token = authorization.substring(7);
            Claims claims = null;
        	
            Map<String, Object> claimsMap = new HashMap<>();
            try {
                claims = parseToken(token);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                claims = e.getClaims(); 
            }
            return TokenInfo.from(claims);
    	}
    	
    	return null;
    }
}
