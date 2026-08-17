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
import com.prafta.web.attd.reqinbox.dto.response.PendingSchedReqListResponse;
import com.prafta.web.attd.reqinbox.dto.response.ProcessedReqListResponse;
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

    /**
     * 매니저 스코프 대기 요청 목록 (reqTypeGroup: correction | overtime | schedule).
     *
     * <p>schedule('10')은 현재→요청 스케줄 비교값을 함께 내려주어 컬럼 세트가 다르므로
     * 전용 서비스/응답으로 분기한다. 응답 필드명은 {@code pendingList} 로 동일하다.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @RequestParam("reqTypeGroup") String reqTypeGroup,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);

        if ("schedule".equals(reqTypeGroup)) {
            PendingSchedReqListResponse schedResponse = PendingSchedReqListResponse.builder()
                    .pendingList(reqInboxService.getPendingSchedRequests(
                            token.gv_cmpnyCd(), token.gv_siteCd(), token.gv_authCd()))
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(schedResponse);
        }

        PendingReqListResponse response = PendingReqListResponse.builder()
                .pendingList(reqInboxService.getPendingRequests(
                        token.gv_cmpnyCd(), token.gv_siteCd(), token.gv_authCd(), reqTypeGroup))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 내 처리 이력 — 로그인 관리자가 승인/반려 처리한 요청 목록
     * (reqTypeGroup: correction | overtime | schedule | leave, 최근 300건).
     *
     * <p>처리자 필터는 토큰의 본인 userCd 로 서버가 강제한다(파라미터 비신뢰).
     */
    @GetMapping("/processed")
    public ResponseEntity<?> processed(
            @RequestParam("reqTypeGroup") String reqTypeGroup,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);

        ProcessedReqListResponse response = reqInboxService.getProcessedRequests(
                token.gv_cmpnyCd(), token.gv_siteCd(), token.gv_userCd(), token.gv_authCd(), reqTypeGroup);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
