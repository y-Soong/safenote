package com.prafta.web.nearmiss.nearmiss01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentInfoParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentListParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.ReclassifyParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.SaveIncidentParam;
import com.prafta.web.nearmiss.nearmiss01.dto.request.ChangeStatusRequest;
import com.prafta.web.nearmiss.nearmiss01.dto.request.IncidentInfoRequest;
import com.prafta.web.nearmiss.nearmiss01.dto.request.IncidentListRequest;
import com.prafta.web.nearmiss.nearmiss01.dto.request.ReclassifyRequest;
import com.prafta.web.nearmiss.nearmiss01.dto.request.SaveIncidentRequest;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.ReclassifyResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.StatusCountResponse;
import com.prafta.web.nearmiss.nearmiss01.service.NearMiss01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아차사고/사건 보고 (웹 관리자) 컨트롤러.
 * 식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다.
 * axios 프리픽스: /webApi/nearmiss01/...
 */
@Slf4j
@RestController
@RequestMapping("/nearmiss01")
@RequiredArgsConstructor
public class NearMiss01Controller {

    private final NearMiss01Service nearMiss01Service;
    private final JwtUtil jwtUtil;

    // E1 사건 목록 (상태탭/유형/잠재중대성/기간 필터 + 사업장 스코프)
    @GetMapping("/incident-lists")
    public ResponseEntity<?> getIncidentLists(
            @ModelAttribute IncidentListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        IncidentListResponse response = nearMiss01Service.selectIncidentList(
            IncidentListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // E2 사건 단건 상세
    @GetMapping("/incident-info")
    public ResponseEntity<?> getIncidentInfo(
            @ModelAttribute IncidentInfoRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        IncidentInfoResponse response = nearMiss01Service.selectIncidentInfo(
            IncidentInfoParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // E3 상태별 카운트 (탭 배지)
    @GetMapping("/status-counts")
    public ResponseEntity<?> getStatusCounts(
            @ModelAttribute IncidentListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        StatusCountResponse response = nearMiss01Service.selectStatusCounts(
            IncidentListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // E4 정밀조사 저장 (원인/재발방지/임시조치)
    @PostMapping(value = "/save-incident", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveIncident(
            @RequestBody SaveIncidentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        nearMiss01Service.saveIncident(
            SaveIncidentParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // E5 상태 전환 (100->200->300->400 / 900 반려)
    @PostMapping(value = "/change-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> changeStatus(
            @RequestBody ChangeStatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        nearMiss01Service.changeStatus(
            ChangeStatusParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // E6 위험성평가요청 -> 아차사고 재분류 (단일 트랜잭션)
    @PostMapping(value = "/reclassify-from-assessment", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reclassifyFromAssessment(
            @RequestBody ReclassifyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ReclassifyResponse response = nearMiss01Service.reclassifyFromAssessment(
            ReclassifyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
