package com.prafta.web.subcon.subcon01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.subcon.subcon01.application.param.CmpnyExactSearchParam;
import com.prafta.web.subcon.subcon01.application.param.RelationCreateParam;
import com.prafta.web.subcon.subcon01.application.param.RelationHistParam;
import com.prafta.web.subcon.subcon01.application.param.RelationListParam;
import com.prafta.web.subcon.subcon01.application.param.RelationProcessParam;
import com.prafta.web.subcon.subcon01.dto.request.CmpnyExactSearchRequest;
import com.prafta.web.subcon.subcon01.dto.request.RelationCreateRequest;
import com.prafta.web.subcon.subcon01.dto.request.RelationHistRequest;
import com.prafta.web.subcon.subcon01.dto.request.RelationProcessRequest;
import com.prafta.web.subcon.subcon01.dto.response.CmpnyExactSearchResponse;
import com.prafta.web.subcon.subcon01.dto.response.RelationCreateResponse;
import com.prafta.web.subcon.subcon01.dto.response.RelationHistResponse;
import com.prafta.web.subcon.subcon01.dto.response.RelationListResponse;
import com.prafta.web.subcon.subcon01.dto.response.TerminateSummaryResponse;
import com.prafta.web.subcon.subcon01.service.Subcon01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회사 간 연동 관계 수립(Subcon_01) 컨트롤러 — PRAFTA-SUBCON-T1.
 *
 * <p>회사 스코프는 JWT 클레임(gv_cmpnyCd)로만 강제한다(클라 바디의 회사코드 불신).
 * 인증은 AuthAspect 가 처리하며, 화면/버튼 인가는 서비스의 TB_SYST_AUTH_MENU 게이트가 강제한다.
 */
@Slf4j
@RestController
@RequestMapping("/subcon01")
@RequiredArgsConstructor
public class Subcon01Controller {

    private final Subcon01Service subcon01Service;
    private final JwtUtil jwtUtil;

    /** 회사 정확일치 조회(3필드 한정 — 미존재/비활성/자기회사 동일한 빈 결과, 열거 방지). */
    @GetMapping("/cmpny-exact-search")
    public ResponseEntity<?> getCmpnyExactSearch(
            @ModelAttribute CmpnyExactSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        CmpnyExactSearchResponse response = subcon01Service.selectCmpnyExact(
                CmpnyExactSearchParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 연동 관계 목록(자사 당사자 전 관계 — 프론트가 연동중/보낸/받은 3분류). */
    @GetMapping("/relation-lists")
    public ResponseEntity<?> getRelationList(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RelationListResponse response = subcon01Service.selectRelationList(
                RelationListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 연동 관계 이력(당사자 검증, 상대사 행위자는 "상대사 처리" 마스킹). */
    @GetMapping("/relation-hists")
    public ResponseEntity<?> getRelationHists(
            @ModelAttribute RelationHistRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RelationHistResponse response = subcon01Service.selectRelationHists(
                RelationHistParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 해지 영향 요약(해지 확인 팝업용 — T1 시점 impacts=[]). */
    @GetMapping("/relation-terminate-summary")
    public ResponseEntity<?> getRelationTerminateSummary(
            @ModelAttribute RelationHistRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TerminateSummaryResponse response = subcon01Service.selectTerminateSummary(
                RelationHistParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 연동 관계 요청 생성(중복 가드 + HIST 동일 트랜잭션). */
    @PostMapping("/relation-request")
    public ResponseEntity<?> registerRelationRequest(
            @RequestBody RelationCreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        RelationCreateResponse response = subcon01Service.insertRelationRequest(
                RelationCreateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 수락(REQUESTED→ACCEPTED, 상대측 소속만). */
    @PostMapping("/relation-accept")
    public ResponseEntity<?> acceptRelation(
            @RequestBody RelationProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon01Service.acceptRelation(
                RelationProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 거부(REQUESTED→REJECTED, 상대측 소속만, 사유 필수 ≤500자). */
    @PostMapping("/relation-reject")
    public ResponseEntity<?> rejectRelation(
            @RequestBody RelationProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon01Service.rejectRelation(
                RelationProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 취소(REQUESTED→CANCELLED, 요청측 소속만). */
    @PostMapping("/relation-cancel")
    public ResponseEntity<?> cancelRelation(
            @RequestBody RelationProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon01Service.cancelRelation(
                RelationProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 해지(ACCEPTED→TERMINATED, 양측 가능 — 해지 자동 종결 훅 동일 트랜잭션). */
    @PostMapping("/relation-terminate")
    public ResponseEntity<?> terminateRelation(
            @RequestBody RelationProcessRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        subcon01Service.terminateRelation(
                RelationProcessParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
