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
import com.prafta.web.attd.leaveflow.application.param.LeaveDeductionPreviewParam;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalActionRequest;
import com.prafta.web.attd.leaveflow.dto.request.LeaveDeductionPreviewRequest;
import com.prafta.web.attd.leaveflow.dto.response.LeaveDeductionPreviewResponse;
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

    /**
     * LC-07(T3): 예상 차감액 미리보기 — INSERT 없음(조회 전용).
     * 검증 가드는 신청과 동일하게 태우고(위반 시 해당 에러 그대로), 잔여 부족은 플래그로 응답한다.
     * 인가: 본인 신청 기준(토큰 gv_userCd)만.
     */
    @PostMapping("/preview-deduction")
    public ResponseEntity<?> previewDeduction(
            @RequestBody @Valid LeaveDeductionPreviewRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        LeaveDeductionPreviewResponse response = leaveFlowService.previewDeduction(
                LeaveDeductionPreviewParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
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
