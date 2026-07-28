package com.prafta.common.cmm.consent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.consent.ConsentConst;
import com.prafta.common.cmm.consent.application.param.ConsentSubconRespondParam;
import com.prafta.common.cmm.consent.application.param.ConsentToggleParam;
import com.prafta.common.cmm.consent.dto.request.ConsentOptionalTermsAgreeRequest;
import com.prafta.common.cmm.consent.dto.request.ConsentSubconRespondRequest;
import com.prafta.common.cmm.consent.dto.response.ConsentAgreeResponse;
import com.prafta.common.cmm.consent.dto.response.ConsentOptionalTermsResponse;
import com.prafta.common.cmm.consent.dto.response.ConsentSubconGateResponse;
import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;
import com.prafta.common.cmm.consent.service.ConsentTermsService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 약관 동의(Consent) 공용 컨트롤러 — 웹 채널 진입점.
 *
 * <p>자동 프리픽스(com.prafta.common.* → /prafta/comApi) 적용 → 실제 매핑:
 *   <ul>
 *     <li>GET  /prafta/comApi/consent/my-optional-terms       — 선택약관 목록(내 정보 팝업 "약관 동의 설정")</li>
 *     <li>POST /prafta/comApi/consent/my-optional-terms-agree — 선택약관 토글(동의/철회)</li>
 *     <li>GET  /prafta/comApi/consent/subcon-consent-gate     — 연동 회사 제3자 제공 동의(006) 게이트 판정</li>
 *     <li>POST /prafta/comApi/consent/subcon-consent-respond  — 위 동의 응답 저장(동의/미동의 모두)</li>
 *   </ul>
 *
 * <p>앱 EP(/appApi/terms01/*)와 <b>동일한 서비스(ConsentTermsService)</b>를 호출한다. 채널별로 판정·저장
 *    경로를 나누면 "앱에서는 동의했는데 웹에서는 안 보이는" 상태가 생기고, 그 차이가 그대로 하도급
 *    공유 스냅샷의 포함/제외 차이가 된다.
 *
 * <p>인증/식별: AuthAspect 가 JWT 를 검증하고(본 컨트롤러는 pointcut 대상), cmpnyCd/userCd 는
 *    JWT 클레임에서만 도출한다(본문/쿼리 식별값 미사용 → IDOR 차단).
 */
@Slf4j
@RestController
@RequestMapping("/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentTermsService consentTermsService;
    private final JwtUtil jwtUtil;

    /** 선택약관 목록(현재버전 + 본인 동의여부). 선택약관이 없으면 빈 배열. */
    @GetMapping("/my-optional-terms")
    public ResponseEntity<?> getMyOptionalTerms(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        return ResponseEntity.status(HttpStatus.OK).body(
                ConsentOptionalTermsResponse.of(
                        consentTermsService.listOptionalTerms(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd())));
    }

    /**
     * 선택약관 토글. body {termsId, agrYn}.
     * 선택약관 여부/현재버전 검증은 서비스가 수행한다(필수약관 우회 차단 → TERMS_403_001).
     * 경로 코드는 MYPAGE — 내 정보 팝업에서의 자발적 변경이다(로그인 게이트 응답과 구분).
     */
    @PostMapping(value = "/my-optional-terms-agree", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> toggleMyOptionalTerms(
            @RequestBody ConsentOptionalTermsAgreeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ConsentToggleParam param = ConsentToggleParam.from(request, jwtUtil.getAllClaimsAsMap(authorization));
        int affected = consentTermsService.toggleOptionalTerms(
                param.cmpnyCd(), param.userCd(), param.termsId(), param.agrYn(), ConsentConst.SOURCE_MYPAGE);

        return ResponseEntity.status(HttpStatus.OK).body(ConsentAgreeResponse.success(param.agrYn(), affected));
    }

    /**
     * 연동 회사 제3자 제공 동의(006) 로그인 게이트 판정.
     * 활성 연동 링크 참여 사업장(SRC 또는 DST) 소속 + 현재버전 미응답일 때만 gateRequiredYn='Y'.
     */
    @GetMapping("/subcon-consent-gate")
    public ResponseEntity<?> getSubconConsentGate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);
        ConsentTermsResult terms = consentTermsService.resolveSubconConsentGate(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK).body(
                terms == null ? ConsentSubconGateResponse.notRequired() : ConsentSubconGateResponse.required(terms));
    }

    /**
     * 연동 회사 제3자 제공 동의(006) 응답 저장. body {agrYn:'Y'|'N'}.
     * 동의/미동의 둘 다 저장한다(행 존재 = 응답 완료 = 게이트 해제). termsId 는 본문으로 받지 않는다(서버 상수).
     */
    @PostMapping(value = "/subcon-consent-respond", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> respondSubconConsent(
            @RequestBody ConsentSubconRespondRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ConsentSubconRespondParam param = ConsentSubconRespondParam.from(request, jwtUtil.getAllClaimsAsMap(authorization));
        int affected = consentTermsService.respondSubconConsent(
                param.cmpnyCd(), param.userCd(), param.agrYn(), ConsentConst.SOURCE_GATE);

        return ResponseEntity.status(HttpStatus.OK).body(ConsentAgreeResponse.success(param.agrYn(), affected));
    }

    /** JWT 클레임 → TokenInfo. userCd 부재면 인증 결함(COMMON_400_003). */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        return tokenInfo;
    }
}
