package com.prafta.app.admin.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.admin.dashboard.application.param.DashboardSummaryParam;
import com.prafta.app.admin.dashboard.dto.response.DashboardSummaryResponse;
import com.prafta.app.admin.dashboard.service.AppAdminDashboardService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * J1-10 (B-5): 모바일 앱 관리자 모드 대시보드 요약 컨트롤러(조회 전용).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET /prafta/appApi/admin/dashboard/summary (현장전환 시 ?siteCd=... 선택)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/admin/dashboard/summary
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/authCd/siteCd 는 JWT 클레임에서만 도출하며
 *   path/query 로 식별자를 받지 않는다. 현장전환 reqSiteCd(선택)만 받되 서비스가 멤버십을 재검증한다.
 *
 * <p>게이트(서버 강제): 진입(master∥hr∥safe∥nodeAdmin) 통과 시 200, 위젯별 권한은 available 로 표현.
 *   근태=노드축(safe ⛔) / 안전3종=사업장축 2단. PII 미노출 — 카운트(숫자)만 반환.
 */
@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AppAdminDashboardController {

    private final AppAdminDashboardService appAdminDashboardService;
    private final JwtUtil jwtUtil;

    /** 대시보드 요약(4영역 카운트 + 위젯별 available). siteCd 선택(현장전환). */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        DashboardSummaryResponse response = appAdminDashboardService.selectSummary(
                DashboardSummaryParam.of(siteCd, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
