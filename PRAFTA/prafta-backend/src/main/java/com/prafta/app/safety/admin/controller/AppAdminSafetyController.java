package com.prafta.app.safety.admin.controller;

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

import com.prafta.app.safety.admin.application.param.InspectionDetailParam;
import com.prafta.app.safety.admin.application.param.InspectionListParam;
import com.prafta.app.safety.admin.application.param.RiskDetailParam;
import com.prafta.app.safety.admin.application.param.RiskFindingListParam;
import com.prafta.app.safety.admin.application.param.RiskStatusParam;
import com.prafta.app.safety.admin.dto.request.RiskStatusRequest;
import com.prafta.app.safety.admin.dto.response.InspectionDetailResponse;
import com.prafta.app.safety.admin.dto.response.InspectionListResponse;
import com.prafta.app.safety.admin.dto.response.RiskDetailResponse;
import com.prafta.app.safety.admin.dto.response.RiskFindingListResponse;
import com.prafta.app.safety.admin.service.AppAdminSafetyService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 모드 안전 관리 컨트롤러 (prafta-app-025 J1-6).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/admin/safety/inspections        (H1 순회점검 결과 리스트)</li>
 *   <li>GET  /prafta/appApi/admin/safety/inspection-detail  (H2 순회점검 상세)</li>
 *   <li>GET  /prafta/appApi/admin/safety/risk-findings      (H3 위험성평가 목록)</li>
 *   <li>GET  /prafta/appApi/admin/safety/risk-detail        (H4 위험성평가 상세)</li>
 *   <li>POST /prafta/appApi/admin/safety/risk-status        (H5 위험성평가 상태전환)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/admin/safety/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/authCd 는 JWT 클레임에서만 도출하며
 *   리소스 사업장(siteCd)/리소스 키(chkptCd/assessmentCd)는 쿼리/바디 수신 후 서버가 스코프를 재강제한다.
 *
 * <p>권한(SAFETY = master ∥ safe ∥ nodeAdmin-in-site, AdminAccessResolver §3.2): EP 가 직접 강제(A-1 비상속).
 * <p>PII(휴대폰/이메일) 미노출 — 점검자/평가자 이름만(직무 식별). 순회점검=조회 전용, 위험성평가=상태전환(쓰기).
 */
@Slf4j
@RestController
@RequestMapping("/admin/safety")
@RequiredArgsConstructor
public class AppAdminSafetyController {

    private final AppAdminSafetyService appAdminSafetyService;
    private final JwtUtil jwtUtil;

    /** H1 순회점검 결과 리스트(workMonth/siteCd 선택). */
    @GetMapping("/inspections")
    public ResponseEntity<?> getInspections(
            @RequestParam(value = "workMonth", required = false) String workMonth,
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        InspectionListResponse response = appAdminSafetyService.selectInspectionList(
                InspectionListParam.of(workMonth, siteCd, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** H2 순회점검 상세(siteCd 선택, chkLstType/chkptCd/workMonth 필수). */
    @GetMapping("/inspection-detail")
    public ResponseEntity<?> getInspectionDetail(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam("chkLstType") String chkLstType,
            @RequestParam("chkptCd") String chkptCd,
            @RequestParam("workMonth") String workMonth,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        InspectionDetailResponse response = appAdminSafetyService.selectInspectionDetail(
                InspectionDetailParam.of(siteCd, chkLstType, chkptCd, workMonth, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** H3 위험성평가 목록(siteCd/assessmentStatus/processCd/riskTypeCd 선택). */
    @GetMapping("/risk-findings")
    public ResponseEntity<?> getRiskFindings(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam(value = "assessmentStatus", required = false) String assessmentStatus,
            @RequestParam(value = "processCd", required = false) String processCd,
            @RequestParam(value = "riskTypeCd", required = false) String riskTypeCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        RiskFindingListResponse response = appAdminSafetyService.selectRiskFindings(
                RiskFindingListParam.of(siteCd, assessmentStatus, processCd, riskTypeCd, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** H4 위험성평가 상세(siteCd 선택, processCd/assessmentCd 필수). */
    @GetMapping("/risk-detail")
    public ResponseEntity<?> getRiskDetail(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam("processCd") String processCd,
            @RequestParam("assessmentCd") String assessmentCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        RiskDetailResponse response = appAdminSafetyService.selectRiskDetail(
                RiskDetailParam.of(siteCd, processCd, assessmentCd, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** H5 위험성평가 상태전환(쓰기 — 허용 전이만, 현재 상태 가드). */
    @PostMapping(value = "/risk-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> changeRiskStatus(
            @RequestBody RiskStatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        appAdminSafetyService.changeRiskStatus(RiskStatusParam.from(request, token));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
