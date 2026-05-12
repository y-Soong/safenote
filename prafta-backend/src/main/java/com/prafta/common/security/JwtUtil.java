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

    public String generateToken(UserResult userResult, String mblNo, String email) {
    	
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
                .claim("gv_mblNo", mblNo)
                .claim("gv_email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
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
    
    public String getMblNoFromToken(String token) {
        return parseToken(token).get("gv_mblNo", String.class);
    }
    
    public String getEmailFromToken(String token) {
        return parseToken(token).get("gv_email", String.class);
    }
    
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
