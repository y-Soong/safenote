package com.prafta.web.leaverefusal.leaverefusal01.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.leaverefusal.leaverefusal01.application.param.LeaveRefusalNoticeParam;
import com.prafta.web.leaverefusal.leaverefusal01.dto.request.LeaveRefusalNoticeRequest;
import com.prafta.web.leaverefusal.leaverefusal01.dto.response.LeaveRefusalNoticeResponse;
import com.prafta.web.leaverefusal.leaverefusal01.service.LeaveRefusalNoticeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 노무수령거부 통지 발송 컨트롤러 (PRAFTA-COM-001 기능1, 웹 관리자).
 *
 * <p>식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출하여 IDOR 을 차단한다(Param.from).
 * axios 프리픽스: /webApi/leave-refusal/...
 */
@Slf4j
@RestController
@RequestMapping("/leave-refusal")
@RequiredArgsConstructor
public class LeaveRefusalController {

    private final LeaveRefusalNoticeService leaveRefusalNoticeService;
    private final JwtUtil jwtUtil;

    /** 사용지정일에 대해 통지 발송(대상 List). 권한 게이트: master/hr. */
    @PostMapping(value = "/notices", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendNotices(
            @RequestBody List<LeaveRefusalNoticeRequest> requests,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        LeaveRefusalNoticeResponse response = leaveRefusalNoticeService.sendNotices(
                LeaveRefusalNoticeParam.from(requests, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
