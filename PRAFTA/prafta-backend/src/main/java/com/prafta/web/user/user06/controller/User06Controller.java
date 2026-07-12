package com.prafta.web.user.user06.controller;

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
import com.prafta.web.user.user06.application.param.BlacklistListParam;
import com.prafta.web.user.user06.application.param.BlacklistRegParam;
import com.prafta.web.user.user06.application.param.BlacklistReleaseParam;
import com.prafta.web.user.user06.dto.request.BlacklistListRequest;
import com.prafta.web.user.user06.dto.request.BlacklistRegRequest;
import com.prafta.web.user.user06.dto.request.BlacklistReleaseRequest;
import com.prafta.web.user.user06.dto.response.BlacklistListResponse;
import com.prafta.web.user.user06.dto.response.BlacklistRegResponse;
import com.prafta.web.user.user06.service.User06Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일일계정 블랙리스트 관리(User_06) 컨트롤러.
 *
 * <p>스코프는 JWT 클레임(gv_cmpnyCd) 로만 강제한다(블랙리스트는 회사 단위). 클라 바디의 회사코드는 신뢰하지 않는다.
 * 인증은 AuthAspect 가 처리하며, 화면 접근 권한(User_06 메뉴)이 있는 사용자는 조회/등록/해제가 가능하다.
 */
@Slf4j
@RestController
@RequestMapping("/user06")
@RequiredArgsConstructor
public class User06Controller {

    private final User06Service user06Service;
    private final JwtUtil jwtUtil;

    /** 블랙리스트 목록 조회(전화번호/사용여부 필터, 회사 스코프, 휴대폰은 서버 마스킹). */
    @GetMapping("/blacklist-lists")
    public ResponseEntity<?> getBlacklistList(
            @ModelAttribute BlacklistListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        BlacklistListResponse response = user06Service.selectBlacklistList(
                BlacklistListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 블랙리스트 등록(평문 전화번호 → 서버에서 정규화/HMAC/ENC/LAST4 파생, 사유 필수). */
    @PostMapping("/blacklist")
    public ResponseEntity<?> registerBlacklist(
            @RequestBody BlacklistRegRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        BlacklistRegResponse response = user06Service.insertBlacklist(
                BlacklistRegParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 블랙리스트 해제(USE_YN 'Y'→'N', 회사 스코프 조건부 UPDATE). */
    @PostMapping("/blacklist-release")
    public ResponseEntity<?> releaseBlacklist(
            @RequestBody BlacklistReleaseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        user06Service.releaseBlacklist(
                BlacklistReleaseParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
