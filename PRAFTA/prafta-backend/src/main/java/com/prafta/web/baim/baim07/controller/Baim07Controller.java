package com.prafta.web.baim.baim07.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim07.application.param.ActivePolicyParam;
import com.prafta.web.baim.baim07.application.param.LeavePolicyHistoryListParam;
import com.prafta.web.baim.baim07.application.param.LeavePolicySaveParam;
import com.prafta.web.baim.baim07.dto.request.LeavePolicyHistoryListRequest;
import com.prafta.web.baim.baim07.dto.request.LeavePolicySaveRequest;
import com.prafta.web.baim.baim07.dto.response.AnalyzeImpactResponse;
import com.prafta.web.baim.baim07.dto.response.ImpactPreviewResponse;
import com.prafta.web.baim.baim07.dto.response.LeavePolicyHistoryListResponse;
import com.prafta.web.baim.baim07.dto.response.LeavePolicyResponse;
import com.prafta.web.baim.baim07.dto.response.LeavePolicySaveResponse;
import com.prafta.web.baim.baim07.service.Baim07Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * baim07 — 회사 법정 연차 부여 정책 관리 (PRAFTA-018, 정책서 §8.5).
 *
 * <p>endpoint:
 * <ul>
 *   <li>GET  /baim07/policy/active           — 활성 정책 단건 조회</li>
 *   <li>POST /baim07/policy                  — 정책 생성</li>
 *   <li>PUT  /baim07/policy/{policySeq}      — 정책 변경</li>
 *   <li>GET  /baim07/policy/history          — 정책 변경 이력 페이징 조회</li>
 *   <li>POST /baim07/policy/impact-preview   — 영향 분석 미리보기 (저장 없음)</li>
 * </ul>
 *
 * <p>권한 가드는 서비스 계층 진입부에서 {@code AuthRoleUtils.isManager(authCd)}로 강제 (§8.5.7).
 * GET endpoints(active, history)는 인증 사용자 + 사업장 스코프 수준만 요구.
 */
@Slf4j
@RestController
@RequestMapping("/baim07")
@RequiredArgsConstructor
public class Baim07Controller {

    private final Baim07Service baim07Service;
    private final JwtUtil jwtUtil;

    /**
     * 활성 정책 단건 조회.
     * 정책서 §8.5.7: 인증 사용자 + 사업장 스코프.
     */
    @GetMapping("/policy/active")
    public ResponseEntity<?> getActivePolicy(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeavePolicyResponse response = baim07Service.getActivePolicy(
                ActivePolicyParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 생성.
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/policy")
    public ResponseEntity<?> createPolicy(
            @RequestBody @Valid LeavePolicySaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeavePolicySaveResponse response = baim07Service.createPolicy(
                LeavePolicySaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 변경.
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     *
     * <p>{@code policySeq}는 변경 대상 정책의 식별자. 서버 측에서 현재 활성 정책과 일치하는지
     * 재검증한다(낙관적 동시성 제어).
     */
    @PutMapping("/policy/{policySeq}")
    public ResponseEntity<?> updatePolicy(
            @PathVariable("policySeq") Long policySeq,
            @RequestBody @Valid LeavePolicySaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeavePolicySaveResponse response = baim07Service.updatePolicy(
                policySeq,
                LeavePolicySaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 변경 이력 페이징 조회.
     * 정책서 §8.5.7: 인증 사용자 + 사업장 스코프.
     */
    @GetMapping("/policy/history")
    public ResponseEntity<?> getHistory(
            @ModelAttribute LeavePolicyHistoryListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeavePolicyHistoryListResponse response = baim07Service.getHistory(
                LeavePolicyHistoryListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 영향 분석 미리보기 (저장 없음).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     */
    @PostMapping("/policy/impact-preview")
    public ResponseEntity<?> previewImpact(
            @RequestBody @Valid LeavePolicySaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ImpactPreviewResponse response = baim07Service.previewImpact(
                LeavePolicySaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 정책 변경 영향 분석 (화면 8, 저장 없음 — §9.9 읽기 전용 시뮬레이션).
     * 정책서 §8.5.7: AUTH_MASTER OR AUTH_HR_MANAGER 필요 (서비스 진입부에서 강제).
     *
     * <p>요청 body는 기존 {@link LeavePolicySaveRequest}(타깃 axis + applyFromDate)를 재사용한다.
     * 현재 정책은 서버가 {@code TB_LEAVE_POLICY WHERE USE_YN='Y'}로 직접 조회한다.
     */
    @PostMapping("/policy/analyze-impact")
    public ResponseEntity<?> analyzeImpact(
            @RequestBody @Valid LeavePolicySaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        AnalyzeImpactResponse response = baim07Service.analyzeImpact(
                LeavePolicySaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
