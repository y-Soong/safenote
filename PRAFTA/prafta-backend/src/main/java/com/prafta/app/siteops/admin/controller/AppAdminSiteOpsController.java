package com.prafta.app.siteops.admin.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.siteops.admin.application.param.SiteOpsQrParam;
import com.prafta.app.siteops.admin.application.param.SiteOpsTargetParam;
import com.prafta.app.siteops.admin.dto.request.SiteOpsQrRequest;
import com.prafta.app.siteops.admin.dto.response.SiteOpsAttendanceResponse;
import com.prafta.app.siteops.admin.dto.response.SiteOpsContractMetaResponse;
import com.prafta.app.siteops.admin.dto.response.SiteOpsSignResponse;
import com.prafta.app.siteops.admin.service.AppAdminSiteOpsService;
import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * J1-7(prafta-app-025) 관리자 현장 일용직 QR 출퇴근 등록 컨트롤러.
 *
 * <p>ApiPrefixConfig 가 {@code com.prafta.app.*} 에 {@code /prafta/appApi} 를 자동 부여 →
 * 최종 {@code /prafta/appApi/admin/site-ops/...}, 프론트 호출 {@code /appApi/admin/site-ops/...}.
 *
 * <p>식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출(IDOR). 진입 게이트/사업장 스코프/대상 유효성/
 * 멱등은 모두 서비스(서버)가 최종 판정한다(C1).
 */
@RestController
@RequestMapping("/admin/site-ops")
@RequiredArgsConstructor
public class AppAdminSiteOpsController {

    private final AppAdminSiteOpsService appAdminSiteOpsService;
    private final JwtUtil jwtUtil;

    /** S1 일용직 QR 출근 등록(멱등). body={ qrPayload, siteCd }. */
    @PostMapping(value = "/attendance/check-in", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checkIn(@RequestBody SiteOpsQrRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        SiteOpsAttendanceResponse response = appAdminSiteOpsService.checkIn(
                SiteOpsQrParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** S2 일용직 QR 퇴근 등록(멱등). body={ qrPayload, siteCd }. */
    @PostMapping(value = "/attendance/check-out", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checkOut(@RequestBody SiteOpsQrRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        SiteOpsAttendanceResponse response = appAdminSiteOpsService.checkOut(
                SiteOpsQrParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ================================================================
    // 현장 계약서 서명 플로우 (2026-08-30) — 출근 SIGN_REQUIRED 후속.
    //   targetUserCd 는 리소스 키일 뿐 인가 근거가 아니다 — 서비스가 관리자 진입 게이트·사업장
    //   멤버십·대상 유효성을 QR 경로와 동일하게 재검증한다(IDOR 차단).
    // ================================================================

    /** 계약서 스트림 캐시 금지(dailycontract01 D-P10 미러) — 성명+자필서명 PII 문서. */
    private static final String CACHE_NO_STORE = "no-store";

    /** 현장 서명 대상 계약서 메타(형식/페이지 수 — pager 초기화). */
    @GetMapping("/contract/meta")
    public ResponseEntity<?> getContractMeta(
            @RequestParam("targetUserCd") String targetUserCd,
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        SiteOpsContractMetaResponse response = appAdminSiteOpsService.findContractMeta(
                SiteOpsTargetParam.from(targetUserCd, siteCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 현장 서명 대상 계약서 단일 페이지 스트림(1-base).
     *
     * <p>page 를 문자열로 받아 400_007 로 통일하는 이유는 dailycontract01 컨트롤러의 parsePage 주석 참조
     * (int 바인딩은 비정수 입력이 전역 catch-all 500 으로 떨어진다).
     */
    @GetMapping("/contract/page")
    public ResponseEntity<?> getContractPage(
            @RequestParam("targetUserCd") String targetUserCd,
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam(value = "page", required = false) String page,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        FileBytesResult file = appAdminSiteOpsService.loadContractPage(
                SiteOpsTargetParam.from(targetUserCd, siteCd, tokenInfo), parsePage(page));

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, CACHE_NO_STORE)
                .body(file.data());
    }

    /**
     * 현장 계약서 서명 저장 — multipart { file(서명 PNG), targetUserCd, siteCd }.
     *
     * <p>서명 주체는 일용직 근로자 <b>본인</b>이다(관리자 폰을 근로자에게 전달해 직접 서명 — 대리 입력 아님,
     * 화면 고지 필수). 이미 서명된 사이클이면 core 가 DAILYCONTRACT_400_002(멱등)를 반환한다.
     */
    @PostMapping(value = "/contract/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> signContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetUserCd") String targetUserCd,
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        SiteOpsSignResponse response = appAdminSiteOpsService.sign(
                SiteOpsTargetParam.from(targetUserCd, siteCd, tokenInfo), file);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** page 파싱(1-base) — 비정수/누락/0 이하는 DAILYCONTRACT_400_007 로 통일(500 노출 금지). */
    private int parsePage(String page) {
        try {
            int parsed = Integer.parseInt(page == null ? "" : page.trim());
            if (parsed < 1) {
                throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_400_007);
        }
    }
}
