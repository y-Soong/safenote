package com.prafta.web.attd.leaveflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalActionRequest;
import com.prafta.web.attd.leaveflow.dto.response.MyApprovalListResponse;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 신청·결재 흐름 컨트롤러 (prafta-019-E).
 * 신청자/결재자는 토큰으로 식별(요청 body의 사용자코드 신뢰 안 함).
 */
@Slf4j
@RestController
@RequestMapping("/leaveflow")
@RequiredArgsConstructor
public class LeaveFlowController {

    private final LeaveFlowService leaveFlowService;
    private final JwtUtil jwtUtil;

    /** 연차 신청. */
    @PostMapping("/apply")
    public ResponseEntity<?> apply(
            @RequestBody @Valid LeaveApplyRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        leaveFlowService.submitLeave(
                LeaveApplyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 결재 단계 승인. */
    @PostMapping("/approve")
    public ResponseEntity<?> approve(
            @RequestBody @Valid LeaveApprovalActionRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        leaveFlowService.approveStep(
                LeaveApprovalActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 결재 단계 반려. */
    @PostMapping("/reject")
    public ResponseEntity<?> reject(
            @RequestBody @Valid LeaveApprovalActionRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        leaveFlowService.rejectStep(
                LeaveApprovalActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 내 결재함: 내가 현재 단계 결재자인 연차 요청 목록 (요청승인관리 연차 탭). */
    @GetMapping("/my-approvals")
    public ResponseEntity<?> myApprovals(
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        MyApprovalListResponse response = MyApprovalListResponse.builder()
                .approvalList(leaveFlowService.getMyPendingLeaveApprovals(
                        token.gv_cmpnyCd(), token.gv_userCd()))
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
