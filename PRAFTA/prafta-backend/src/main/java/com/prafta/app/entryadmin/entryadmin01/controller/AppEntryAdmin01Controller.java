package com.prafta.app.entryadmin.entryadmin01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.entryadmin.entryadmin01.application.param.EntryApproveParam;
import com.prafta.app.entryadmin.entryadmin01.application.param.EntryPendingListParam;
import com.prafta.app.entryadmin.entryadmin01.application.param.EntryRejectParam;
import com.prafta.app.entryadmin.entryadmin01.dto.request.EntryApproveRequest;
import com.prafta.app.entryadmin.entryadmin01.dto.request.EntryRejectRequest;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryPendingListResponse;
import com.prafta.app.entryadmin.entryadmin01.dto.response.EntryProcessResponse;
import com.prafta.app.entryadmin.entryadmin01.service.AppEntryAdmin01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 관리자 일용직 입장 승인 컨트롤러 (일용직 계약서+승인제 T2, UI-DC-03).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/entryadmin01/pending-lists (당일 승인 대기 목록)</li>
 *   <li>POST /prafta/appApi/entryadmin01/approve       (일괄/개별 승인 — D9)</li>
 *   <li>POST /prafta/appApi/entryadmin01/reject        (거부 + 사유 — D10)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/entryadmin01/...
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/siteCd/userCd/authCd 는 JWT 클레임에서만
 * 도출하며 body/query 로 식별자를 받지 않는다. reqId 는 리소스 키이며 core 가 사업장 인가를 재검증한다.
 */
@Slf4j
@RestController
@RequestMapping("/entryadmin01")
@RequiredArgsConstructor
public class AppEntryAdmin01Controller {

    private final AppEntryAdmin01Service appEntryAdmin01Service;
    private final JwtUtil jwtUtil;

    /** 승인 대기('01') 목록 — 사업장 스코프는 JWT gv_siteCd 로 강제. */
    @GetMapping("/pending-lists")
    public ResponseEntity<?> getPendingList(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EntryPendingListResponse response = appEntryAdmin01Service.selectPendingList(
                EntryPendingListParam.from(token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 일괄/개별 승인 처리. body={reqIds[]}. all-or-nothing(하나라도 실패 시 전체 롤백). */
    @PostMapping("/approve")
    public ResponseEntity<?> approve(
            @RequestBody EntryApproveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EntryProcessResponse response = appEntryAdmin01Service.approve(
                EntryApproveParam.of(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 거부 처리. body={reqId, reason}. 사유 필수(200자 이하) — 일용직에게는 미노출(내부 기록). */
    @PostMapping("/reject")
    public ResponseEntity<?> reject(
            @RequestBody EntryRejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EntryProcessResponse response = appEntryAdmin01Service.reject(
                EntryRejectParam.of(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
