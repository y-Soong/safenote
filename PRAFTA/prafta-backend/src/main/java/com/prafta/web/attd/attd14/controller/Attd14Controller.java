package com.prafta.web.attd.attd14.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd14.application.param.AdminRequestHistoryListParam;
import com.prafta.web.attd.attd14.dto.request.AdminRequestHistoryListRequest;
import com.prafta.web.attd.attd14.dto.response.AdminRequestHistoryDetailResponse;
import com.prafta.web.attd.attd14.dto.response.AdminRequestHistoryListResponse;
import com.prafta.web.attd.attd14.result.AdminRequestHistoryRowResult;
import com.prafta.web.attd.attd14.service.Attd14Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자 발신 연차 변경 요청 이력 — 읽기 전용 컨트롤러 (prafta-com-016-H).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.web.* 에 /prafta/webApi 자동 부여):
 * <ul>
 *   <li>GET /prafta/webApi/attd14/admin-requests             (관리자 발신 이력 목록·검색·페이징)</li>
 *   <li>GET /prafta/webApi/attd14/admin-requests/{changeReqId} (단건 상세, 읽기 전용)</li>
 * </ul>
 * 식별값(회사/권한/사용자)은 JWT 에서만 도출(IDOR 차단). 스코프/권한은 attd13 정책 계승(safe 제외).
 * 본 컨트롤러는 쓰기 EP 가 전혀 없다(발의/확인/반려는 attd13).
 */
@Slf4j
@RestController
@RequestMapping("/attd14")
@RequiredArgsConstructor
public class Attd14Controller {

    private final Attd14Service attd14Service;
    private final JwtUtil jwtUtil;

    /** 관리자 발신 요청 이력 목록(검색/필터/페이징). */
    @GetMapping("/admin-requests")
    public ResponseEntity<?> getAdminRequests(
            @ModelAttribute AdminRequestHistoryListRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        AdminRequestHistoryListResponse response = attd14Service.getAdminRequestHistory(
                AdminRequestHistoryListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 관리자 발신 요청 이력 단건 상세(읽기 전용). 스코프 밖이면 404. */
    @GetMapping("/admin-requests/{changeReqId}")
    public ResponseEntity<?> getAdminRequestDetail(
            @PathVariable("changeReqId") String changeReqId,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        AdminRequestHistoryRowResult detail = attd14Service.getAdminRequestHistoryDetail(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd(), tokenInfo.gv_userCd(), changeReqId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(AdminRequestHistoryDetailResponse.builder().detail(detail).build());
    }
}
