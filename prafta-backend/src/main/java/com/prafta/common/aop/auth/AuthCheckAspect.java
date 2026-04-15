package com.prafta.common.aop.auth;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuthCheckAspect {

    private final JwtUtil jwtUtil;

    // @AuthCheck 어노테이션이 붙은 메서드를 대상으로 지정
    @Pointcut("@annotation(com.prafta.common.aop.auth.AuthCheck)")
    public void authCheckTarget() {}

    @Before("authCheckTarget()")
    public void validateToken() {
        HttpServletRequest request =
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        	throw new ApiException(CommonErrorCode.COMMON_400_600);
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
        	throw new ApiException(CommonErrorCode.COMMON_400_600);
        }

        log.info("인증 성공: {}", jwtUtil.getUserCdFromToken(token));
    }
}
