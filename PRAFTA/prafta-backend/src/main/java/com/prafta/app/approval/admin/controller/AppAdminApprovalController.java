package com.prafta.app.approval.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.approval.admin.application.param.ApprovalDetailParam;
import com.prafta.app.approval.admin.application.param.ApprovalHistoryParam;
import com.prafta.app.approval.admin.application.param.ApprovalPendingParam;
import com.prafta.app.approval.admin.application.param.ApprovalProcessParam;
import com.prafta.app.approval.admin.dto.request.ApprovalProcessRequest;
import com.prafta.app.approval.admin.dto.response.ApprovalDetailResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalHistoryResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalPendingResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalProcessResponse;
import com.prafta.app.approval.admin.service.AppAdminApprovalService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 001-P2: 모바일 앱 관리자 모드 승인 관리 컨트롤러(읽기/게이트 — A-1/A-2/A-5).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/admin/approval/pending  (A-1 대기 리스트 + counts)</li>
 *   <li>GET  /prafta/appApi/admin/approval/detail   (A-2 상세 + gate)</li>
 *   <li>GET  /prafta/appApi/admin/approval/history  (A-5 이력)</li>
 *   <li>POST /prafta/appApi/admin/approval/process  (A-3 처리: 승인/조정후승인/반려)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/admin/approval/...
 *
 * <p>인증/IDOR(D1): AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/authCd/siteCd 는 JWT 클레임에서만
 *   도출하며 path/query/body 로 식별자를 받지 않는다. reqId 는 리소스 키이며 서버가 토큰 스코프 내인지 재검증한다.
 *
 * <p>경계: 선점잠금(A-4 lock/unlock)·스케줄(10) 승인은 본 라운드 범위 외(후속 R4/R7).
 *   처리(A-3)의 APPROVE_ADJUST(조정후승인)는 본 라운드 보류(후속 R3).
 */
@Slf4j
@RestController
@RequestMapping("/admin/approval")
@RequiredArgsConstructor
public class AppAdminApprovalController {

    private final AppAdminApprovalService appAdminApprovalService;
    private final JwtUtil jwtUtil;

    /** A-1 대기 리스트(group=ALL|CORRECTION|OVERTIME|LEAVE, sort=REQUESTED|DEADLINE, keyword, page, pageSize). */
    @GetMapping("/pending")
    public ResponseEntity<?> getPending(
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ApprovalPendingResponse response = appAdminApprovalService.selectPending(
                ApprovalPendingParam.of(group, sort, keyword, page, pageSize, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A-2 상세 + gate(본인결재차단/마감/충돌). reqId 토큰 스코프 재검증(IDOR). */
    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(
            @RequestParam("reqId") String reqId,
            @RequestParam(value = "group", required = false) String group,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ApprovalDetailResponse response = appAdminApprovalService.selectDetail(
                ApprovalDetailParam.of(reqId, group, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** A-5 이력(group, startDate, endDate, keyword, page, pageSize). 정렬 PROCESS_DATE DESC 고정. */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ApprovalHistoryResponse response = appAdminApprovalService.selectHistory(
                ApprovalHistoryParam.of(group, startDate, endDate, keyword, page, pageSize, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * A-3 처리(승인/조정후승인/반려). body={reqId, group, decision, approvalStep?, comment?, adjusted?}.
     * 식별자(cmpny/site/user/auth)는 JWT 클레임에서만 도출하며 body 로 받지 않는다(IDOR 차단).
     * 처리 전 서버에서 본인결재차단·마감·멱등(409)·스코프를 재검증한다.
     */
    @PostMapping("/process")
    public ResponseEntity<?> process(
            @RequestBody ApprovalProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ApprovalProcessResponse response = appAdminApprovalService.process(
                ApprovalProcessParam.of(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
