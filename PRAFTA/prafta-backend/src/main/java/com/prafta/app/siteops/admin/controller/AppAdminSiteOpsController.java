package com.prafta.app.siteops.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.siteops.admin.application.param.SiteOpsQrParam;
import com.prafta.app.siteops.admin.dto.request.SiteOpsQrRequest;
import com.prafta.app.siteops.admin.dto.response.SiteOpsAttendanceResponse;
import com.prafta.app.siteops.admin.service.AppAdminSiteOpsService;
import com.prafta.common.dto.TokenInfo;
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
}
