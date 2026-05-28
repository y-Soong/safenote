package com.prafta.web.attd.reqinbox.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.reqinbox.dto.response.PendingReqListResponse;
import com.prafta.web.attd.reqinbox.service.ReqInboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 요청 승인 관리 통합 대기요청 컨트롤러 (prafta-019 후속).
 * 스코프(회사/사업장)는 토큰 기반. 반려/승인은 기존 attd07 엔드포인트 사용.
 */
@Slf4j
@RestController
@RequestMapping("/reqinbox")
@RequiredArgsConstructor
public class ReqInboxController {

    private final ReqInboxService reqInboxService;
    private final JwtUtil jwtUtil;

    /** 매니저 스코프 대기 요청 목록 (reqTypeGroup: correction | overtime). */
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @RequestParam("reqTypeGroup") String reqTypeGroup,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        PendingReqListResponse response = PendingReqListResponse.builder()
                .pendingList(reqInboxService.getPendingRequests(
                        token.gv_cmpnyCd(), token.gv_siteCd(), token.gv_authCd(), reqTypeGroup))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
