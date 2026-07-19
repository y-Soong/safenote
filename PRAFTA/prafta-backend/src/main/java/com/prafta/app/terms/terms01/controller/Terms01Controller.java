package com.prafta.app.terms.terms01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.terms.terms01.application.param.OptionalTermsAgreeParam;
import com.prafta.app.terms.terms01.application.param.SubconConsentRespondParam;
import com.prafta.app.terms.terms01.dto.request.OptionalTermsAgreeRequest;
import com.prafta.app.terms.terms01.dto.request.SubconConsentRespondRequest;
import com.prafta.app.terms.terms01.dto.response.OptionalTermsResponse;
import com.prafta.app.terms.terms01.dto.response.PendingTermsResponse;
import com.prafta.app.terms.terms01.dto.response.SubconConsentGateResponse;
import com.prafta.app.terms.terms01.dto.response.SubconConsentRespondResponse;
import com.prafta.app.terms.terms01.dto.response.TermsAgreeResponse;
import com.prafta.app.terms.terms01.service.Terms01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 약관(Terms) 컨트롤러.
 *
 * <p>자동 프리픽스(com.prafta.app.* → /prafta/appApi) 적용 → 실제 매핑:
 *   <ul>
 *     <li>GET  /prafta/appApi/terms01/required-terms-pending — 미동의 필수약관(로그인 게이트)</li>
 *     <li>POST /prafta/appApi/terms01/agree-required-terms   — 필수약관 일괄 동의</li>
 *     <li>GET  /prafta/appApi/terms01/optional-terms         — 선택약관 목록(마이페이지)</li>
 *     <li>POST /prafta/appApi/terms01/optional-terms-agree   — 선택약관 토글</li>
 *     <li>GET  /prafta/appApi/terms01/subcon-consent-gate    — 연동 회사 제3자 제공 동의 게이트 판정(SUBCON-T4)</li>
 *     <li>POST /prafta/appApi/terms01/subcon-consent-respond — 위 동의 응답 저장(동의/미동의 모두)(SUBCON-T4)</li>
 *   </ul>
 * <p>인증/식별: AuthAspect 가 JWT 를 검증하고, 본 컨트롤러는 jwtUtil.getAllClaimsAsMap(Authorization)
 *    → TokenInfo 로 USER_CD 를 도출한다(IDOR 차단). 로그인 직후 호출도 정식 토큰 보유 상태이므로 정상 인증된다.
 */
@Slf4j
@RestController
@RequestMapping("/terms01")
@RequiredArgsConstructor
public class Terms01Controller {

    private final Terms01Service terms01Service;
    private final JwtUtil jwtUtil;

    /** 미동의 필수약관 조회(로그인 게이트). 빈 목록이면 게이트 불필요. */
    @GetMapping("/required-terms-pending")
    public ResponseEntity<?> getRequiredTermsPending(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        PendingTermsResponse response = terms01Service.selectPendingRequiredTerms(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 필수약관 일괄 동의. 서버가 미동의 목록을 재산출하여 전부 AGR_YN='Y' upsert(멱등). */
    @PostMapping("/agree-required-terms")
    public ResponseEntity<?> agreeRequiredTerms(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        TermsAgreeResponse response = terms01Service.agreeRequiredTerms(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 선택약관 목록(마이페이지). 현재버전 + 사용자 동의여부 포함. */
    @GetMapping("/optional-terms")
    public ResponseEntity<?> getOptionalTerms(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        OptionalTermsResponse response = terms01Service.selectOptionalTerms(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 선택약관 토글. body {termsId, agrYn}. 선택약관 검증은 서비스가 수행(필수약관 우회 차단). */
    @PostMapping(value = "/optional-terms-agree", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> toggleOptionalTerms(
            @RequestBody OptionalTermsAgreeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        TermsAgreeResponse response = terms01Service.toggleOptionalTerms(
                OptionalTermsAgreeParam.from(request, tokenInfo));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 연동 회사 제3자 제공 동의(006) 로그인 게이트 판정(SUBCON-T4).
     * 활성 연동 링크 참여 사업장(SRC 또는 DST) 소속 + 현재버전 미응답일 때만 gateRequiredYn='Y'.
     */
    @GetMapping("/subcon-consent-gate")
    public ResponseEntity<?> getSubconConsentGate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        SubconConsentGateResponse response = terms01Service.selectSubconConsentGate(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 연동 회사 제3자 제공 동의(006) 응답 저장(SUBCON-T4). body {agrYn:'Y'|'N'}.
     * 동의/미동의 둘 다 저장(행 존재 = 응답 완료 = 게이트 해제). termsId 는 본문으로 받지 않는다(서버 상수).
     */
    @PostMapping(value = "/subcon-consent-respond", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> respondSubconConsent(
            @RequestBody SubconConsentRespondRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        SubconConsentRespondResponse response = terms01Service.respondSubconConsent(
                SubconConsentRespondParam.from(request, tokenInfo));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** JWT 클레임 → TokenInfo. userCd 부재면 인증 결함(COMMON_400_003). */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        return tokenInfo;
    }
}
