package com.prafta.web.user.user09.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;
import com.prafta.web.user.user09.application.param.SelfJoinApproveParam;
import com.prafta.web.user.user09.application.param.SelfJoinListParam;
import com.prafta.web.user.user09.application.param.SelfJoinRejectParam;
import com.prafta.web.user.user09.dto.request.SelfJoinApproveRequest;
import com.prafta.web.user.user09.dto.request.SelfJoinListRequest;
import com.prafta.web.user.user09.dto.request.SelfJoinRejectRequest;
import com.prafta.web.user.user09.dto.response.SelfJoinListResponse;
import com.prafta.web.user.user09.service.User09Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소정-09: 셀프가입 승인/거부 (User_09) 컨트롤러.
 *
 * <p>실제 매핑(자동 프리픽스 {@code com.prafta.web.*} → {@code /prafta/webApi}):
 * <ul>
 *   <li>GET  /prafta/webApi/user09/self-join-lists   — 신청 목록('06' 승인대기 / '07' 거부)</li>
 *   <li>POST /prafta/webApi/user09/self-join-approve — 승인(인사정보 보강 + 소정근로 이력)</li>
 *   <li>POST /prafta/webApi/user09/self-join-reject  — 거부(사유는 감사 로그 보존)</li>
 * </ul>
 *
 * <p>회사/권한/요청자/토큰 사업장은 <b>JWT 클레임에서만</b> 도출한다. 승인·거부 대상의 사업장/부서는
 * 서비스가 DB 에서 재조회해 권한 게이트 입력으로 쓴다(바디 값 불신 — IDOR 차단).
 */
@Slf4j
@RestController
@RequestMapping("/user09")
@RequiredArgsConstructor
public class User09Controller {

    private final User09Service user09Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/self-join-lists")
    public ResponseEntity<?> getSelfJoinList(
            @ModelAttribute SelfJoinListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        SelfJoinListResponse response = user09Service.selectSelfJoinList(
                SelfJoinListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/self-join-approve")
    public ResponseEntity<?> approveSelfJoin(
            @RequestBody SelfJoinApproveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {

        user09Service.approveSelfJoin(
                SelfJoinApproveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)),
                buildAuditContext(httpRequest));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/self-join-reject")
    public ResponseEntity<?> rejectSelfJoin(
            @RequestBody SelfJoinRejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {

        user09Service.rejectSelfJoin(
                SelfJoinRejectParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)),
                buildAuditContext(httpRequest));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 감사 컨텍스트(IP/UA) 추출 — Service 가 HttpServletRequest 에 의존하지 않도록 여기서 만든다. */
    private AuditContext buildAuditContext(HttpServletRequest httpRequest) {
        return new AuditContext(
                ClientIpExtractor.extract(httpRequest),
                httpRequest != null ? httpRequest.getHeader("User-Agent") : null);
    }
}
