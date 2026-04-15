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

    @Before("execution(* com.prafta..controller..*(..)) " +
            "&& !execution(* com.prafta.common.cmm.login.controller..*(..)) " +
            "&& !execution(* com.prafta.common.cmm.baseinfo.controller..*(..)) " +
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
