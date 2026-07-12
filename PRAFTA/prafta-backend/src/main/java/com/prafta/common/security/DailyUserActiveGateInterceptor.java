package com.prafta.common.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.prafta.common.cmm.login.mapper.LoginMapper;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.dailylogin.DailyLoginErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일용직 계정 비활성화 즉시 차단 게이트(로그인 이후 후속 요청 재검증).
 *
 * <p>배경: 본 프로젝트는 중앙 JWT 인증 필터가 없어, 발급된 토큰은 만료 전까지 그대로 통한다.
 * 따라서 관리자가 슬롯 비우기 등으로 일용직 계정을 비활성화해도, 이미 로그인한 단말은 토큰 만료
 * 전까지 출퇴근 등 기능을 계속 쓸 수 있었다. 이를 막기 위해 {@code /appApi/**} 요청마다 토큰이
 * 일용직이면 계정 활성 상태(TB_USER)를 재검증하고, 비활성이면 {@code DAILYLOGIN_403_001} 로 차단한다.
 *
 * <p>범위(사용자 결정): 일용직(JWT {@code gv_employmentType='DAILY'})만. 정규 사용자는 즉시 통과
 * (DB 조회 없음 → 성능/회귀 0). 토큰이 없거나 파싱 실패(위조/malformed 등)면 통과시켜 각 엔드포인트의
 * 기존 인증 흐름에 위임한다.
 *
 * <p>참고(만료 토큰): {@code JwtUtil.getAllClaimsAsMap} 은 만료 토큰도 예외 없이 claims 를 반환한다.
 * 따라서 만료된 일용직 토큰도 게이트가 활성 검증을 수행한다 — 활성이면 통과 후 다운스트림에서 401(refresh),
 * 비활성이면 여기서 403 으로 차단된다(어차피 차단 대상이라 무방). refresh 경로(refreshToken 본문, Authorization
 * 미동봉)는 본 게이트가 못 막으므로 {@code AuthServiceImpl.refreshAccessToken} 에서 별도로 비활성 일용직을 차단한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyUserActiveGateInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final LoginMapper loginMapper;

    /** ACCOUNT_STATUS '05' = 비활성화(로그인 서비스와 동일 상수). */
    private static final String ACCOUNT_STATUS_INACTIVE = "05";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return true; // 토큰 없음 — 다운스트림(엔드포인트) 인증 흐름에 위임.
        }

        TokenInfo token;
        try {
            token = jwtUtil.getAllClaimsAsMap(authorization);
        } catch (Exception e) {
            // 토큰 파싱 실패(만료/위조 등) — 게이트가 가로채지 않고 기존 401(refresh) 흐름에 위임.
            return true;
        }

        // 일용직 토큰만 대상(정규 사용자는 즉시 통과 — DB 조회 없음).
        if (token == null || !AuthRoleUtils.isDailyWorker(token.gv_employmentType())) {
            return true;
        }

        String cmpnyCd = token.gv_cmpnyCd();
        String userCd = token.gv_userCd();
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd)) {
            return true; // 식별자 불완전 — 위임(가로채지 않음).
        }

        // 통합형 일용직의 인가/활성 단일 출처 = TB_USER. 관리자 슬롯 비우기 시 TB_USER 도 비활성(USE_YN='N', '05')된다.
        //   (selectUserByUserCd 는 USE_YN 미필터 — 활성이면 행 반환, 비활성이면 useYn='N'/status='05' 로 식별.)
        UserResult user = loginMapper.selectUserByUserCd(cmpnyCd, userCd);
        boolean inactive = user == null
                || !"Y".equals(user.useYn())
                || ACCOUNT_STATUS_INACTIVE.equals(user.accountStatus());
        if (inactive) {
            // 평문 PII 로그 금지 — 식별 키만 남긴다.
            log.warn("[일용직 게이트] 비활성 계정 후속 요청 차단 — cmpnyCd={}, userCd={}", cmpnyCd, userCd);
            throw new ApiException(DailyLoginErrorCode.DAILYLOGIN_403_001);
        }
        return true;
    }
}
