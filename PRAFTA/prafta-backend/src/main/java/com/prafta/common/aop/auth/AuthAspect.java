package com.prafta.common.aop.auth;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.auth.mapper.AuthMapper;
import com.prafta.common.error.auth.AuthErrorCode;
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
    private final AuthMapper authMapper; // prafta-057: 다른 환경 로그인 감지(LOGIN_ID 패밀리 조회)

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

        // prafta-057: 다른 환경 로그인 감지(WEB 한정).
        //   신규 로그인은 같은 계정·같은 WEB 클라이언트의 기존 세션 RT 를 모두 폐기한다(LoginServiceImpl.revokeActiveToken).
        //   따라서 현재 토큰의 LOGIN_ID 패밀리에 활성 토큰이 하나도 없으면 "다른 환경에서 로그인됨" 으로 판정하여 강제 로그아웃시킨다.
        //   - WEB 판정은 조작 가능한 X-Client-Type 헤더가 아니라 발급 시 서명된 gv_clientType 클레임으로 한다(헤더 위장 우회 차단).
        //   - 비 WEB(앱 등) 클라이언트, gv_loginId 클레임이 없는 레거시 토큰은 검사하지 않는다(무회귀·앱 무영향).
        //   - 만료 토큰은 위 validateToken 단계에서 이미 COMMON_400_600(=리프레시 경로)로 처리되므로 여기 도달하지 않는다.
        String clientType = claims.get("gv_clientType", String.class);
        if ("WEB".equals(clientType)) {
            String loginId = claims.get("gv_loginId", String.class);
            if (loginId != null && !loginId.isBlank()) {
                String cmpnyCd = claims.get("gv_cmpnyCd", String.class);
                String userCd = claims.get("gv_userCd", String.class);
                int active = authMapper.countActiveByLoginId(cmpnyCd, userCd, loginId);
                if (active == 0) {
                    throw new ApiException(AuthErrorCode.AUTH_409_001);
                }
            }
        }
    }
}
