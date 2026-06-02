package com.prafta.app.nearmiss.nearmiss01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.IncidentDetailParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.MyReportListParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.ReportParam;
import com.prafta.app.nearmiss.nearmiss01.application.param.SiteIncidentListParam;
import com.prafta.app.nearmiss.nearmiss01.dto.request.ChangeStatusRequest;
import com.prafta.app.nearmiss.nearmiss01.dto.request.IncidentDetailRequest;
import com.prafta.app.nearmiss.nearmiss01.dto.request.MyReportListRequest;
import com.prafta.app.nearmiss.nearmiss01.dto.request.ReportRequest;
import com.prafta.app.nearmiss.nearmiss01.dto.request.SiteIncidentListRequest;
import com.prafta.app.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.ReportResponse;
import com.prafta.app.nearmiss.nearmiss01.dto.response.StatusCountResponse;
import com.prafta.app.nearmiss.nearmiss01.service.AppNearMiss01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아차사고/사건 보고 (앱) 컨트롤러.
 *
 * <p>식별자(cmpnyCd/siteCd/userCd/authCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다.
 *    base URL: ApiPrefixConfig 자동 프리픽스 → /appApi/nearmiss/...
 * <p>앱 완전분리 원칙(app-010): web 컨트롤러(/webApi/nearmiss01/*) 를 호출하지 않는다.
 */
@Slf4j
@RestController
@RequestMapping("/nearmiss")
@RequiredArgsConstructor
public class AppNearMiss01Controller {

    private final AppNearMiss01Service appNearMiss01Service;
    private final JwtUtil jwtUtil;

    // A1 근로자 아차사고 보고 등록 (multipart/form-data, 단일 사진)
    @PostMapping(value = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> report(
            @ModelAttribute ReportRequest request
            , @RequestPart(value = "item", required = false) MultipartFile file
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        ReportResponse response = appNearMiss01Service.report(
            ReportParam.from(request, file, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // A2 내 보고 목록 (본인)
    @GetMapping("/my-reports")
    public ResponseEntity<?> getMyReports(
            @ModelAttribute MyReportListRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        IncidentListResponse response = appNearMiss01Service.selectMyReports(
            MyReportListParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // A3 관리자 사업장 사건 목록 (사업장 스코프 + 필터)
    @GetMapping("/site-incidents")
    public ResponseEntity<?> getSiteIncidents(
            @ModelAttribute SiteIncidentListRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        IncidentListResponse response = appNearMiss01Service.selectSiteIncidents(
            SiteIncidentListParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // A4 관리자 상태별 카운트 (탭 배지)
    @GetMapping("/status-counts")
    public ResponseEntity<?> getStatusCounts(
            @ModelAttribute SiteIncidentListRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        StatusCountResponse response = appNearMiss01Service.selectStatusCounts(
            SiteIncidentListParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // A5 사건 단건 상세 (본인 보고건 OR 사업장 관리자)
    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(
            @ModelAttribute IncidentDetailRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        IncidentInfoResponse response = appNearMiss01Service.selectIncidentInfo(
            IncidentDetailParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // A6 1차 확인 상태전환 (100->200 + 임시조치 / 900 반려)
    @PostMapping(value = "/change-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> changeStatus(
            @RequestBody ChangeStatusRequest request
            , @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appNearMiss01Service.changeStatus(
            ChangeStatusParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
