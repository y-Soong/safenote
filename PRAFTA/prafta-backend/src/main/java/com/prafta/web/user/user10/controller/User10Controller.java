package com.prafta.web.user.user10.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user10.application.param.StdWorkSaveParam;
import com.prafta.web.user.user10.application.param.StdWorkUserListParam;
import com.prafta.web.user.user10.dto.request.StdWorkSaveRequest;
import com.prafta.web.user.user10.dto.request.StdWorkUserListRequest;
import com.prafta.web.user.user10.service.User10Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-10: 소정근로시간 관리·이력 (User_10) 컨트롤러.
 *
 * <p>실제 매핑(자동 프리픽스 {@code com.prafta.web.*} → {@code /prafta/webApi}):
 * <ul>
 *   <li>GET  /prafta/webApi/user10/std-work-user-lists    — 대상 근로자 + 현재 소정</li>
 *   <li>GET  /prafta/webApi/user10/std-work-histories     — 특정 근로자 이력 타임라인 + 요약</li>
 *   <li>GET  /prafta/webApi/user10/std-work-reason-options— 등록 팝업 옵션(SYS083 전 사유)</li>
 *   <li>POST /prafta/webApi/user10/std-work-register      — 이력 등록(계약 변경)</li>
 *   <li>POST /prafta/webApi/user10/std-work-correct       — 이력 정정(오입력)</li>
 * </ul>
 *
 * <p>★본 컨트롤러의 EP 는 전부 <b>타인 스코프</b>(userCd 입력)다. 본인 스코프 EP
 * ({@code /comApi/leave-feature/std-work-summary})와 달리 서비스가 사업장 인가 +
 * {@code canManageNodeExcludeSafe} 부서 게이트를 반드시 건다
 * (feedback_web_new_query_screen_needs_node_gate).
 */
@Slf4j
@RestController
@RequestMapping("/user10")
@RequiredArgsConstructor
public class User10Controller {

    private final User10Service user10Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/std-work-user-lists")
    public ResponseEntity<?> getStdWorkUserList(
            @ModelAttribute StdWorkUserListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                user10Service.selectStdWorkUserList(
                        StdWorkUserListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    @GetMapping("/std-work-histories")
    public ResponseEntity<?> getStdWorkHistory(
            @RequestParam("userCd") String userCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);

        return ResponseEntity.status(HttpStatus.OK).body(
                user10Service.selectStdWorkHistory(
                        tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd(), tokenInfo.gv_userCd(),
                        tokenInfo.gv_siteCd(), userCd));
    }

    /** 옵션 조회 — 회사 스코프는 토큰에서만 도출(파라미터 없음). */
    @GetMapping("/std-work-reason-options")
    public ResponseEntity<?> getReasonOptions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = resolveToken(authorization);

        return ResponseEntity.status(HttpStatus.OK).body(
                user10Service.getReasonOptions(tokenInfo.gv_cmpnyCd()));
    }

    @PostMapping("/std-work-register")
    public ResponseEntity<?> registerStdWorkHours(
            @RequestBody StdWorkSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                user10Service.registerStdWorkHours(
                        StdWorkSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    @PostMapping("/std-work-correct")
    public ResponseEntity<?> correctStdWorkHours(
            @RequestBody StdWorkSaveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                user10Service.correctStdWorkHours(
                        StdWorkSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
    }

    /** JWT 클레임 → TokenInfo. 회사/사용자 식별 부재면 인증 결함(COMMON_400_003). */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()
                || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return tokenInfo;
    }
}
