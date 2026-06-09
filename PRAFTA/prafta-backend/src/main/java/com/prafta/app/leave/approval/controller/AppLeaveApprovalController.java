package com.prafta.app.leave.approval.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.leave.approval.application.param.LeaveApprovalDetailParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalHistoryParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalPendingParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalProcessParam;
import com.prafta.app.leave.approval.dto.request.LeaveApprovalProcessRequest;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalDetailResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalHistoryResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalPendingResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalProcessResponse;
import com.prafta.app.leave.approval.service.AppLeaveApprovalService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자연차결재-01: 모바일 앱 사용자 모드 연차 결재 관리 컨트롤러(결재자 본인 스코프).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/leaveflow/approval/pending  (대기 리스트)</li>
 *   <li>GET  /prafta/appApi/leaveflow/approval/detail   (상세 + gate)</li>
 *   <li>GET  /prafta/appApi/leaveflow/approval/history  (내 처리 내역)</li>
 *   <li>POST /prafta/appApi/leaveflow/approval/process  (승인/반려)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/leaveflow/approval/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/siteCd 는 JWT 클레임에서만 도출하며
 *   path/query/body 로 식별자를 받지 않는다. reqId/approvalStep/keyword/기간만 리소스 키·필터로 수신하고,
 *   상세·처리는 서버가 결재선 실존(isApproverOf)을 재검증한다. 관리자 승인 관리(/admin/approval/*) 와 무관.
 */
@Slf4j
@RestController
@RequestMapping("/leaveflow/approval")
@RequiredArgsConstructor
public class AppLeaveApprovalController {

    private final AppLeaveApprovalService appLeaveApprovalService;
    private final JwtUtil jwtUtil;

    /** 3-A 대기 리스트(keyword: 요청자명/사번). */
    @GetMapping("/pending")
    public ResponseEntity<?> getPending(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        LeaveApprovalPendingResponse response = appLeaveApprovalService.selectPending(
                LeaveApprovalPendingParam.from(token, keyword));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 3-B 상세 + gate. reqId 결재선 실존 재검증(IDOR). */
    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(
            @RequestParam("reqId") String reqId,
            @RequestParam(value = "approvalStep", required = false) Integer approvalStep,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        LeaveApprovalDetailResponse response = appLeaveApprovalService.selectDetail(
                LeaveApprovalDetailParam.of(reqId, approvalStep, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 3-C 처리 내역(startDate/endDate YYYYMMDD, 기본 최근 30일 + keyword). 정렬 처리일시 DESC 고정. */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        LeaveApprovalHistoryResponse response = appLeaveApprovalService.selectHistory(
                LeaveApprovalHistoryParam.of(startDate, endDate, keyword, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 3-D 처리(승인/반려). body={reqId, approvalStep, decision(APPROVE|REJECT), comment?}.
     * 식별자는 JWT 클레임에서만 도출한다(IDOR). 진입 시 isApproverOf 게이트 후 표준 엔진 위임(@Transactional).
     */
    @PostMapping("/process")
    public ResponseEntity<?> process(
            @RequestBody LeaveApprovalProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        LeaveApprovalProcessResponse response = appLeaveApprovalService.process(
                LeaveApprovalProcessParam.of(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
