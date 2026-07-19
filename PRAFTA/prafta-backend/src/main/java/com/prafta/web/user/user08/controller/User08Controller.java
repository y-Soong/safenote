package com.prafta.web.user.user08.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.file.application.model.ImageBytesResult;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.dailycontract.DailyContractErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user08.application.param.ContractSignListParam;
import com.prafta.web.user.user08.application.param.EntryApproveParam;
import com.prafta.web.user.user08.application.param.EntryRejectParam;
import com.prafta.web.user.user08.application.param.EntryRequestListParam;
import com.prafta.web.user.user08.dto.request.ContractSignListRequest;
import com.prafta.web.user.user08.dto.request.EntryApproveRequest;
import com.prafta.web.user.user08.dto.request.EntryRejectRequest;
import com.prafta.web.user.user08.dto.request.EntryRequestListRequest;
import com.prafta.web.user.user08.dto.response.ContractSignListResponse;
import com.prafta.web.user.user08.dto.response.EntryProcessResponse;
import com.prafta.web.user.user08.dto.response.EntryRequestListResponse;
import com.prafta.web.user.user08.service.User08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹 User_08(일용직 입장 승인 + 서명 이력) 컨트롤러 (일용직 계약서+승인제 T2, UI-DC-06).
 *
 * <p>엔드포인트 (프론트 호출 = /webApi/user08/...):
 * <ul>
 *   <li>GET  /user08/entry-request-lists — 승인요청 목록(사업장/상태/유형/요청일 필터)</li>
 *   <li>POST /user08/entry-approve — 일괄/개별 승인(D9)</li>
 *   <li>POST /user08/entry-reject — 거부 + 사유(D10)</li>
 *   <li>GET  /user08/contract-sign-lists — 계약서 서명 이력(탭2, T3 확장)</li>
 *   <li>GET  /user08/contract-sign-image?signId= — 서명본 합성 이미지 스트림(열람/다운로드, T3 확장)</li>
 * </ul>
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. 회사/처리자 식별은 JWT 클레임에서만 도출하며,
 * reqId/siteCd/signId 는 리소스 키로 받되 core(DailyEntryService/DailyContractService)가
 * 사업장 인가를 재검증한다. 서명본 파일 경로는 응답에 노출하지 않는다(스트림 응답만).
 */
@Slf4j
@RestController
@RequestMapping("/user08")
@RequiredArgsConstructor
public class User08Controller {

    private final User08Service user08Service;
    private final JwtUtil jwtUtil;

    /** 입장 승인요청 목록 조회 (siteCd 필수, reqStatus/reqType/reqDate 선택 — 화면 기본 요청일=오늘). */
    @GetMapping("/entry-request-lists")
    public ResponseEntity<?> getEntryRequestList(
            @ModelAttribute EntryRequestListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EntryRequestListResponse response = user08Service.selectEntryRequestList(
                EntryRequestListParam.from(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 일괄/개별 승인 처리. body={reqIds[]}. all-or-nothing(하나라도 실패 시 전체 롤백). */
    @PostMapping("/entry-approve")
    public ResponseEntity<?> approve(
            @RequestBody EntryApproveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EntryProcessResponse response = user08Service.approve(
                EntryApproveParam.of(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 거부 처리. body={reqId, reason}. 사유 필수(200자 이하) — 일용직에게는 미노출(내부 기록). */
    @PostMapping("/entry-reject")
    public ResponseEntity<?> reject(
            @RequestBody EntryRejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EntryProcessResponse response = user08Service.reject(
                EntryRejectParam.of(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 계약서 서명 이력 목록 (탭2 — siteCd 필수, 기간/이름 선택. 만료/탈퇴 계정 서명본도 조회 가능 §6-2). */
    @GetMapping("/contract-sign-lists")
    public ResponseEntity<?> getContractSignList(
            @ModelAttribute ContractSignListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ContractSignListResponse response = user08Service.selectContractSignList(
                ContractSignListParam.from(request, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 서명본 합성 이미지 스트림 (열람/다운로드 — signId 사업장 인가 가드는 core 수행, IDOR 차단). */
    @GetMapping("/contract-sign-image")
    public ResponseEntity<?> getContractSignImage(
            @RequestParam("signId") String signId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        ImageBytesResult image = user08Service.loadContractSignImage(
                signId, token.gv_cmpnyCd(), token.gv_userCd(), token.gv_authCd());
        if (image == null) {
            // DB 메타는 있으나 디스크 파일 유실 등 — 존재 비노출 통일(404).
            throw new ApiException(DailyContractErrorCode.DAILYCONTRACT_404_001);
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(image.mediaType()))
                .body(image.data());
    }
}
