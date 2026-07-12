package com.prafta.common.aop.auth;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.auth.mapper.AuthMapper;
import com.prafta.common.cmm.auth.result.AuthGateUserResult;
import com.prafta.common.error.auth.AuthErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.platform.common.PlatformConstants;

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

        // 임시 scope 토큰(gv_scope 보유)은 게이트(휴대폰 본인인증/약관 동의/기본 근무타입 등) 전용이며
        //   일반 API 인증에는 절대 사용할 수 없다. 정식 로그인 토큰엔 gv_scope 가 없으므로, gv_scope 가
        //   존재하면 게이트 미완료 상태에서 새로고침/조작으로 우회하려는 시도로 보고 차단한다.
        //   (게이트 전용 endpoint 는 login/auth 컨트롤러 또는 @NoAuth 로 본 Aspect 에서 이미 면제되어 무영향)
        String gvScope = claims.get("gv_scope", String.class);
        if (gvScope != null && !gvScope.isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_600);
        }

        // 클라이언트 구분은 조작 가능한 X-Client-Type 헤더가 아니라 발급 시 서명된 gv_clientType 클레임으로 한다.
        //   prafta-app-033(게이트 강제) / prafta-057(다른 환경 로그인 감지) 양쪽이 공유한다.
        String clientType = claims.get("gv_clientType", String.class);

        // prafta-app-033: 로그인 게이트(이용약관 동의 / 강제 비밀번호 변경) 서버 강제.
        //   권고형 게이트(로그인 시 정식 토큰을 이미 발급하고 FE 팝업만 띄움)는 새로고침/조작으로 우회되므로
        //   인증 통과한 사용자라도 게이트 미해소 상태면 일반 API 를 차단한다(FE 는 AUTH_403_001 수신 시 로그아웃→재로그인).
        //   ★데드락 방지: 게이트 해소 전용 EP 는 화이트리스트로 허용(아니면 사용자가 게이트를 풀 수 없어 영구 잠김).
        //     - 웹 약관 EP(/comApi/login/*), 로그아웃, update-auth-menu-info 는 login.controller 라 본 Aspect pointcut 에서
        //       이미 면제됨(주석만). 명시 허용이 필요한 건 update-my-passwd(webApi)와 앱 terms01/비번 EP(appApi).
        //   ★무한 로그아웃 루프 방지: gv_clientType 별로 그 클라이언트의 해소 플로우와 동일한 정의로 판정한다.
        //     - WEB 약관: USE_YN='Y' 전체 미동의(selectUserTermsAgreementCheck 동치)
        //     - APP/일용직/clientType 없음 약관: REQUIRED_YN='Y' 미동의(selectPendingRequiredTerms 동치)
        //     - 비번 게이트: PWD_CHG_DTIME IS NULL 이고 EMPLOYMENT_TYPE != 'DAILY' 이며 clientType ∈ {WEB, APP}.
        //       (일용직은 비번변경 흐름이 없어 제외 — 강제 시 해소 불가 데드락)
        if (!isGateResolveWhitelisted(request.getRequestURI())) {
            String gvCmpnyCd = claims.get("gv_cmpnyCd", String.class);
            String gvUserCd  = claims.get("gv_userCd", String.class);
            if (gvUserCd != null && !gvUserCd.isBlank()) {
                AuthGateUserResult gateUser = authMapper.selectGateUserInfo(gvCmpnyCd, gvUserCd);
                if (gateUser != null) {
                    boolean gatePending = false;
                    // 사용자에게 1회 안내할 정확한 사유(내부 용어 "게이트" 배제). 미해소 항목에 따라 채운다.
                    String gateReason = null;

                    // 1) 강제 비밀번호 변경(정규직 WEB/APP 한정).
                    boolean isDaily = "DAILY".equals(gateUser.employmentType());
                    if (("WEB".equals(clientType) || "APP".equals(clientType))
                            && !isDaily
                            && "Y".equals(gateUser.pwdChgPendingYn())) {
                        gatePending = true;
                        gateReason = "비밀번호 변경이 필요합니다.\n다시 로그인해 주세요.";
                    }

                    // 2) 이용약관 동의 게이트(클라별 정의 일치).
                    //    ★ 플랫폼 운영자(prafta_system_admin)는 전역 약관을 '관리'하는 주체이지 '동의' 대상이 아니다.
                    //      운영자가 약관(USE_YN='Y')을 등록하면 본인이 미동의 상태가 되어 모든 API 가 잠기고
                    //      (FE 가 AUTH_403_001 수신 → 로그아웃 → 토큰 없이 /platformApi 재호출 → PLATFORM_403_001),
                    //      운영자에겐 약관 '동의' 플로우 자체가 없어 영구 데드락이 된다. 따라서 운영자는 약관 게이트에서 제외한다.
                    //      (강제 비밀번호 변경 게이트는 유지 — 화이트리스트로 해소 가능, 데드락 아님)
                    if (!gatePending && !PlatformConstants.PLATFORM_CMPNY_CD.equals(gvCmpnyCd)) {
                        // cmpnyCd 는 JWT 클레임(gv_cmpnyCd)에서 도출한 서버 신뢰값만 사용(회사 스코프 동의 판정).
                        int termsPending = "WEB".equals(clientType)
                                ? authMapper.countWebPendingTerms(gvCmpnyCd, gvUserCd)
                                : authMapper.countAppPendingRequiredTerms(gvCmpnyCd, gvUserCd);
                        if (termsPending > 0) {
                            gatePending = true;
                            gateReason = "이용약관 동의가 필요합니다.\n다시 로그인해 주세요.";
                        }
                    }

                    if (gatePending) {
                        // 정확한 사유를 detailMessage 로 전달(enum 기본 메시지를 대체) → FE 가 그대로 1회 안내.
                        throw new ApiException(AuthErrorCode.AUTH_403_001, gateReason);
                    }
                }
            }
        }

        // prafta-057: 다른 환경 로그인 감지(WEB 한정).
        //   신규 로그인은 같은 계정·같은 WEB 클라이언트의 기존 세션 RT 를 모두 폐기한다(LoginServiceImpl.revokeActiveToken).
        //   따라서 현재 토큰의 LOGIN_ID 패밀리에 활성 토큰이 하나도 없으면 "다른 환경에서 로그인됨" 으로 판정하여 강제 로그아웃시킨다.
        //   - WEB 판정은 조작 가능한 X-Client-Type 헤더가 아니라 발급 시 서명된 gv_clientType 클레임으로 한다(헤더 위장 우회 차단).
        //   - 비 WEB(앱 등) 클라이언트, gv_loginId 클레임이 없는 레거시 토큰은 검사하지 않는다(무회귀·앱 무영향).
        //   - 만료 토큰은 위 validateToken 단계에서 이미 COMMON_400_600(=리프레시 경로)로 처리되므로 여기 도달하지 않는다.
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

    // prafta-app-033: 로그인 게이트 해소 전용 EP 화이트리스트.
    //   게이트 미해소 상태여도 아래 EP 는 허용해야 사용자가 게이트를 풀 수 있다(영구 잠김 방지).
    //   JWT 검증은 이미 통과한 상태이므로 여기서는 "게이트 서브체크"만 skip 한다.
    //   요청 URI 는 컨텍스트(/prafta)·prefix(/webApi,/appApi)가 앞에 붙으므로 endsWith(말단 정확매칭)로 판정한다.
    //   (보강 033-001: contains→endsWith. 화이트리스트 문자열을 substring 으로 포함하는 무관 EP 가 추가돼도
    //    게이트가 우회되지 않도록 한다. 현재 모든 항목이 경로 말단이고 getRequestURI 는 쿼리스트링 미포함이라 정확.)
    private static final String[] GATE_RESOLVE_WHITELIST = {
            "/webApi/user01/update-my-passwd"        // 웹 강제 비밀번호 변경
            , "/appApi/mypage/password"               // 앱 강제 비밀번호 변경(기존 EP 재사용)
            , "/appApi/terms01/required-terms-pending" // 앱 미동의 필수약관 조회
            , "/appApi/terms01/agree-required-terms"   // 앱 필수약관 일괄 동의
            // 앱 셸 부트스트랩(메뉴 트리). 앱 게이트 화면(/TermsAgree, /ForcedPasswordChange)은 보호 라우트라
            //   router beforeEach 의 injectDynamicRoutes 가 이 EP 를 호출한다. 게이트로 막으면 게이트 화면 진입
            //   자체가 403 → 강제 로그아웃되어 사용자가 게이트를 풀 수 없다(데드락). 읽기 전용 메뉴 메타데이터일
            //   뿐이며 실제 액션/데이터 EP 는 모두 게이트가 유지되므로 게이트 취지(사용 차단)는 보존된다.
            //   (웹은 게이트를 로그인 페이지 팝업으로 처리하여 해소 전 web-menu-lists 를 호출하지 않으므로 불필요)
            , "/comApi/baseinfo/app-menu-lists"
    };

    private boolean isGateResolveWhitelisted(String uri) {
        if (uri == null) {
            return false;
        }
        for (String allowed : GATE_RESOLVE_WHITELIST) {
            if (uri.endsWith(allowed)) {
                return true;
            }
        }
        return false;
    }
}
