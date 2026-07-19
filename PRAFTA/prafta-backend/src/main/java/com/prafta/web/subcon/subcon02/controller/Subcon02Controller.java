package com.prafta.web.subcon.subcon02.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.subcon.subcon02.application.param.ChkptLinkParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkListParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkProcessParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkProposeParam;
import com.prafta.web.subcon.subcon02.dto.request.ChkptLinkRequest;
import com.prafta.web.subcon.subcon02.dto.request.SiteLinkProcessRequest;
import com.prafta.web.subcon.subcon02.dto.request.SiteLinkProposeRequest;
import com.prafta.web.subcon.subcon02.dto.response.LinkProposeCandidatesResponse;
import com.prafta.web.subcon.subcon02.dto.response.SiteLinkListResponse;
import com.prafta.web.subcon.subcon02.dto.response.SiteLinkProposeResponse;
import com.prafta.web.subcon.subcon02.service.Subcon02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사업장 연동 관리(Subcon_02) 컨트롤러 — PRAFTA-SUBCON-T2.
 *
 * <p>회사 스코프는 JWT 클레임(gv_cmpnyCd)로만 강제한다(클라 바디의 회사코드 불신).
 * 인증은 AuthAspect 가 처리하며, 화면/버튼 인가는 서비스의 TB_SYST_AUTH_MENU 게이트가 강제한다.
 */
@Slf4j
@RestController
@RequestMapping("/subcon02")
@RequiredArgsConstructor
public class Subcon02Controller {

    private final Subcon02Service subcon02Service;
    private final JwtUtil jwtUtil;

    /** 사업장 연동 링크 목록(자사 당사자 전 상태 — 프론트가 제공/받은 2분류, 목록=이력). */
    @GetMapping("/site-link-lists")
    public ResponseEntity<?> getSiteLinkList(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        SiteLinkListResponse response = subcon02Service.selectSiteLinkList(
                SiteLinkListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 연동 제안 후보(관계 ACCEPTED 상대 회사 + 내 활성 사업장 — 미러 포함 재제안 허용). */
    @GetMapping("/link-propose-candidates")
    public ResponseEntity<?> getLinkProposeCandidates(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LinkProposeCandidatesResponse response = subcon02Service.selectProposeCandidates(
                SiteLinkListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 연동 제안 생성(가드 5종: 소유/관계/자기회사/루프/중복 — 전부 서버 강제). */
    @PostMapping("/site-link-propose")
    public ResponseEntity<?> proposeSiteLink(
            @RequestBody SiteLinkProposeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        SiteLinkProposeResponse response = subcon02Service.proposeSiteLink(
                SiteLinkProposeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 수락(PROPOSED→ACTIVE, 수신측 소속만 — 미러 생성 단일 트랜잭션). */
    @PostMapping("/site-link-accept")
    public ResponseEntity<?> acceptSiteLink(
            @RequestBody SiteLinkProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon02Service.acceptSiteLink(
                SiteLinkProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 거부(PROPOSED→REJECTED, 수신측 소속만, 사유 필수 ≤500자). */
    @PostMapping("/site-link-reject")
    public ResponseEntity<?> rejectSiteLink(
            @RequestBody SiteLinkProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon02Service.rejectSiteLink(
                SiteLinkProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 취소(PROPOSED→CANCELLED, 제안측 소속만). */
    @PostMapping("/site-link-cancel")
    public ResponseEntity<?> cancelSiteLink(
            @RequestBody SiteLinkProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon02Service.cancelSiteLink(
                SiteLinkProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 해지(ACTIVE→TERMINATED, 양측 가능 — 수신 미러 독립화 동반, 하위 체인 무접촉). */
    @PostMapping("/site-link-terminate")
    public ResponseEntity<?> terminateSiteLink(
            @RequestBody SiteLinkProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon02Service.terminateSiteLink(
                SiteLinkProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * PRAFTA-SUBCON-T6-02: 순회점검 구성 연동 실행(제공측 SRC 소속만 — 점검대상/문항 미러 생성 + 체인 재귀 확장).
     * 조건 불충족(미존재/타사/이미 연동)은 전부 404 로 통합한다(존재 비노출).
     */
    @PostMapping("/chkpt-link-enable")
    public ResponseEntity<?> enableChkptLink(
            @RequestBody ChkptLinkRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon02Service.enableChkptLink(
                ChkptLinkParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** PRAFTA-SUBCON-T6-02: 순회점검 구성 연동 해제(양측 가능 — 점검 미러 독립화, 결과 통합 중단). */
    @PostMapping("/chkpt-link-disable")
    public ResponseEntity<?> disableChkptLink(
            @RequestBody ChkptLinkRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon02Service.disableChkptLink(
                ChkptLinkParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
