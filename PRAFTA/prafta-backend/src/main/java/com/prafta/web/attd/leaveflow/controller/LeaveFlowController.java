package com.prafta.web.attd.leaveflow.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.model.FileBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.EmploymentTypeGuard;
import com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveDeductionPreviewParam;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalActionRequest;
import com.prafta.web.attd.leaveflow.dto.request.LeaveApprovalBulkRequest;
import com.prafta.web.attd.leaveflow.dto.request.LeaveDeductionPreviewRequest;
import com.prafta.web.attd.leaveflow.dto.response.LeaveDeductionPreviewResponse;
import com.prafta.web.attd.leaveflow.dto.response.MyApprovalListResponse;
import com.prafta.web.attd.leaveflow.service.LeaveApprovalBulkService;
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
    /** prafta-leavemulti: 일괄 승인/반려(별도 빈 — 건별 트랜잭션 분리를 위해 프록시 경유가 필요). */
    private final LeaveApprovalBulkService leaveApprovalBulkService;
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

    /**
     * prafta-leavemulti: 연차 결재 <b>일괄</b> 승인.
     *
     * <p>기간(From-To) 신청은 날짜별 REQ N건으로 분해되므로 2주 휴가면 14번 승인해야 한다.
     * 묶음(또는 화면에서 선택한 임의 건)을 한 번에 처리한다.
     *
     * <p><b>부분 성공</b>이다 — 1건이 마감 등으로 막혀도 나머지는 확정된다.
     * 판정·권한 가드는 전부 단건 경로가 그대로 수행하며, 실패 건은 사유와 함께 응답에 담긴다.
     */
    @PostMapping("/approve-bulk")
    public ResponseEntity<?> approveBulk(
            @RequestBody @Valid LeaveApprovalBulkRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                leaveApprovalBulkService.approveBulk(jwtUtil.getAllClaimsAsMap(authorization), request));
    }

    /** prafta-leavemulti: 연차 결재 일괄 반려. 규약은 일괄 승인과 동일(부분 성공). */
    @PostMapping("/reject-bulk")
    public ResponseEntity<?> rejectBulk(
            @RequestBody @Valid LeaveApprovalBulkRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                leaveApprovalBulkService.rejectBulk(jwtUtil.getAllClaimsAsMap(authorization), request));
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

    // ============================================================
    // 연차 신청 증빙 필수화(2026-08-29): 업로드/제출 분리 아키텍처 (앱 AppLeaveFlowController 미러)
    // ============================================================

    /**
     * 증빙 파일 업로드(신청과 별도). 반환된 fileMgmtCd 를 {@code /apply} 요청 바디의
     * {@code evidenceFileId} 로 실어 보낸다.
     */
    @PostMapping(value = "/evidence-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadEvidenceFile(
            @RequestPart(value = "item", required = false) MultipartFile file,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        // 일용직은 연차 신청 비해당이므로 증빙 업로드 자체도 서버에서 차단
        // (앱은 security Medium 으로 사후 추가됐던 가드 — 웹은 최초 구현부터 반영).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);

        String fileMgmtCd = leaveFlowService.uploadEvidenceFile(tokenInfo, file);

        return ResponseEntity.status(HttpStatus.OK).body(java.util.Map.of("fileMgmtCd", fileMgmtCd));
    }

    /**
     * 증빙 파일 열람 — 본인(업로드 신청자) 또는 해당 요청 결재선에 포함된 결재자만 접근 가능(스코프 검증은
     * 서비스 계층). 공개 정적 URL 금지, 인증 스트림 서빙(SEC-1 — 근로계약서와 동일 원칙).
     */
    @GetMapping("/evidence-file/{fileMgmtCd}")
    public ResponseEntity<byte[]> getEvidenceFile(
            @PathVariable("fileMgmtCd") String fileMgmtCd,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        FileBytesResult file = leaveFlowService.loadEvidenceFile(tokenInfo, fileMgmtCd);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.data());
    }
}
