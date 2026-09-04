package com.prafta.app.mypage.mypage01.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.mypage.mypage01.application.param.ApprovalCandidateParam;
import com.prafta.app.mypage.mypage01.application.param.BrkWaiveStandingUpdateParam;
import com.prafta.app.mypage.mypage01.application.param.MobileSendParam;
import com.prafta.app.mypage.mypage01.application.param.MobileVerifyParam;
import com.prafta.app.mypage.mypage01.application.param.PasswordChangeParam;
import com.prafta.app.mypage.mypage01.application.param.PresetActionParam;
import com.prafta.app.mypage.mypage01.application.param.PresetSaveParam;
import com.prafta.app.mypage.mypage01.application.param.ProfileUpdateParam;
import com.prafta.app.mypage.mypage01.application.param.UpdateDefaultSchParam;
import com.prafta.app.mypage.mypage01.dto.request.ApprovalCandidateRequest;
import com.prafta.app.mypage.mypage01.dto.request.BrkWaiveStandingRequest;
import com.prafta.app.mypage.mypage01.dto.request.MobileSendRequest;
import com.prafta.app.mypage.mypage01.dto.request.MobileVerifyRequest;
import com.prafta.app.mypage.mypage01.dto.request.PasswordChangeRequest;
import com.prafta.app.mypage.mypage01.dto.request.PresetActionRequest;
import com.prafta.app.mypage.mypage01.dto.request.PresetSaveRequest;
import com.prafta.app.mypage.mypage01.dto.request.ProfileUpdateRequest;
import com.prafta.app.mypage.mypage01.dto.request.UpdateDefaultSchRequest;
import com.prafta.app.mypage.mypage01.dto.response.MypageProfileEditResponse;
import com.prafta.app.mypage.mypage01.service.AppMypage01Service;
import com.prafta.common.cmm.sms.policy.SmsClientIpResolver;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-010: 모바일 앱 마이페이지 컨트롤러 (mypage01).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여) → /prafta/appApi/mypage/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 검증. CMPNY_CD/USER_CD 는 JWT 에서만 도출하며 바디/쿼리로 받지 않는다.
 */
@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class AppMypage01Controller {

    private final AppMypage01Service appMypage01Service;
    private final JwtUtil jwtUtil;
    /** SMS2-B2/B4: SMS 상한 IP 축 전용 IP 해석기(확정 불가 시 null → IP 축 스킵). */
    private final SmsClientIpResolver smsClientIpResolver;

    /** 010-01: 마이페이지 메인 프로필(마스킹). */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.getProfile(tokenInfo));
    }

    /** 010-01b: 개인정보 수정 진입 전용 프로필(복호화 전체). no-store 캐시. */
    @GetMapping("/profile/edit")
    public ResponseEntity<?> getProfileForEdit(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        MypageProfileEditResponse response = appMypage01Service.getProfileForEdit(tokenInfo);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /** 010-02: 프로필 저장. */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        appMypage01Service.updateProfile(
                ProfileUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("success", true));
    }

    /** 010-03a: 휴대폰 변경 인증번호 발송(앱 전용). */
    // SMS2-B4: IP 축 상한 재료(해시)를 컨트롤러에서 해석해 Param 으로 넘긴다(서비스는 HttpServletRequest 미의존).
    @PostMapping("/mobile/request-verification")
    public ResponseEntity<?> requestMobileVerification(
            @RequestBody MobileSendRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpServletRequest) {

        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.sendMobileVerification(
                MobileSendParam.from(request, jwtUtil.getAllClaimsAsMap(authorization),
                        smsClientIpResolver.resolveIpHash(httpServletRequest))));
    }

    /** 010-03b: 휴대폰 변경 인증 검증(앱 전용, 로그인 토큰 미발급). */
    @PostMapping("/mobile/verify")
    public ResponseEntity<?> verifyMobile(
            @RequestBody MobileVerifyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.verifyMobile(
                MobileVerifyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    /** 010-04: 비밀번호 변경. */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordChangeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        appMypage01Service.changePassword(
                PasswordChangeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("success", true));
    }

    /** 010-05: 결재선 프리셋 목록. */
    @GetMapping("/approval-presets")
    public ResponseEntity<?> getPresets(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                appMypage01Service.getPresets(jwtUtil.getAllClaimsAsMap(authorization)));
    }

    /** 010-05: 결재선 프리셋 단건. */
    @GetMapping("/approval-presets/{presetId}")
    public ResponseEntity<?> getPreset(
            @PathVariable("presetId") String presetId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                appMypage01Service.getPreset(jwtUtil.getAllClaimsAsMap(authorization), presetId));
    }

    /** 010-05: 결재선 프리셋 저장(신규/수정). */
    @PostMapping("/approval-presets")
    public ResponseEntity<?> savePreset(
            @RequestBody PresetSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.savePreset(
                PresetSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    /** 010-05: 결재선 프리셋 기본 지정. */
    @PostMapping("/approval-presets/set-default")
    public ResponseEntity<?> setDefaultPreset(
            @RequestBody PresetActionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        appMypage01Service.setDefaultPreset(
                PresetActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("success", true));
    }

    /** 010-05: 결재선 프리셋 삭제. */
    @PostMapping("/approval-presets/delete")
    public ResponseEntity<?> deletePreset(
            @RequestBody PresetActionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        appMypage01Service.deletePreset(
                PresetActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("success", true));
    }

    /** 010-05: 결재자 후보 목록. */
    @GetMapping("/approval-candidates")
    public ResponseEntity<?> getApprovalCandidates(
            @ModelAttribute ApprovalCandidateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.getApprovalCandidates(
                ApprovalCandidateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ===== F-8-2: 본인 기본 근무타입 자기변경(세션 사업장 고정) =====

    /** 선택지 조회 — 대상 사업장은 세션 토큰 식별 사용자의 SITE_CD 로만 도출(파라미터 없음). */
    @GetMapping("/default-sch-options")
    public ResponseEntity<?> getDefaultSchOptions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.getDefaultSchOptions(tokenInfo));
    }

    /**
     * PRAFTA-002(기본근무타입-승인제): 요청 등록 — 대상 회사/사용자는 세션 토큰에서만 도출(IDOR 방지).
     * URL/메서드 무변경(2026-08-26 확정, 구버전 앱 호환) — 응답 바디만 요청 식별값/상태로 교체.
     */
    @PostMapping("/update-default-sch")
    public ResponseEntity<?> updateDefaultSch(
            @RequestBody UpdateDefaultSchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.updateDefaultSch(
                UpdateDefaultSchParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    // ===== BW-12(§7-1): 휴게 미이용 상시 요청(근기법 제54조① 단서) — 근로자 본인 전용 =====

    /**
     * 현행값 조회 — {@code { standingYn, standingDtime, eligibleYn }}.
     * 대상 회사/사용자는 세션 토큰에서만 도출한다(관리자 대리 조회 경로 없음).
     */
    @GetMapping("/brk-waive-standing")
    public ResponseEntity<?> getBrkWaiveStanding(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        return ResponseEntity.status(HttpStatus.OK)
                .body(appMypage01Service.getBrkWaiveStanding(tokenInfo));
    }

    /**
     * 상시 요청 저장 — 본문 {@code { standingYn: 'Y'|'N' }}. 현행값 UPDATE + 이력 INSERT 가 한 트랜잭션.
     * 값이 'Y'/'N' 이 아니거나 일용직(DAILY)이면 ATTD_400_220.
     */
    @PutMapping("/brk-waive-standing")
    public ResponseEntity<?> updateBrkWaiveStanding(
            @RequestBody BrkWaiveStandingRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(appMypage01Service.updateBrkWaiveStanding(
                BrkWaiveStandingUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }
}
