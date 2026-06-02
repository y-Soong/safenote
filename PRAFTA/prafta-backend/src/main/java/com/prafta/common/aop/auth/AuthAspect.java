package com.prafta.common.aop.auth;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthAspect {

    private final HttpServletRequest request;
    private final JwtUtil jwtUtil; // 🔹 JwtUtil 주입

    // baseinfo 컨트롤러 통째 면제 절은 제거됨(PRAFTA-006-005 005-3-C).
    // baseinfo 의 공개(비로그인) 엔드포인트는 메서드 레벨 @NoAuth 로 개별 면제하고,
    // 로그인-후 엔드포인트는 본 Aspect 가 JWT 를 강제한다.
    @Before("execution(* com.prafta..controller..*(..)) " +
            "&& !execution(* com.prafta.common.cmm.login.controller..*(..)) " +
            "&& !execution(* com.prafta.common.cmm.auth.controller..*(..)) " +
            "&& !@annotation(com.prafta.common.annotation.NoAuth)")
    public void checkAuthorization(JoinPoint joinPoint) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
        	throw new ApiException(CommonErrorCode.COMMON_400_600);
        }

        String pureToken = token.substring(7);

        if (!jwtUtil.validateToken(pureToken)) {
        	throw new ApiException(CommonErrorCode.COMMON_400_600);
        }

        // 필요하면 Claims 활용 가능
        Claims claims = jwtUtil.parseToken(pureToken);
        // claims.get("gv_userCd", String.class) 같은 식으로 사용 가능
    }
}
