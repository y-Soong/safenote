package com.prafta.web.attd.attd13.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd13.application.param.ChangeRequestConfirmParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestCreateParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestListParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestRejectParam;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestCreateRequest;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestListRequest;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestRejectRequest;
import com.prafta.web.attd.attd13.dto.response.ChangeRequestDetailResponse;
import com.prafta.web.attd.attd13.dto.response.ChangeRequestListResponse;
import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;
import com.prafta.web.attd.attd13.service.Attd13Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 변경/삭제 동의·거부 — 관리자(웹) 컨트롤러 (PRAFTA-COM-008-C).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.web.* 에 /prafta/webApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/webApi/attd13/change-requests          (목록·검색)</li>
 *   <li>POST /prafta/webApi/attd13/change-requests          (관리자 발의 MOVE/DELETE)</li>
 *   <li>POST /prafta/webApi/attd13/change-requests/{id}/confirm (관리자 확인)</li>
 *   <li>POST /prafta/webApi/attd13/change-requests/{id}/reject  (관리자 반려·WORKER 발의건)</li>
 * </ul>
 * 식별값(회사/권한/사용자)은 JWT 에서만 도출(IDOR 차단). siteCd 는 토큰 사업장과 일치 강제.
 */
@Slf4j
@RestController
@RequestMapping("/attd13")
@RequiredArgsConstructor
public class Attd13Controller {

    private final Attd13Service attd13Service;
    private final JwtUtil jwtUtil;

    /** 변경 요청 목록 조회(검색: 사업장/소속부서+하위/사용자명/상태). */
    @GetMapping("/change-requests")
    public ResponseEntity<?> getChangeRequests(
            @ModelAttribute ChangeRequestListRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        List<LeaveChangeRequestRowResult> list = attd13Service.getChangeRequests(
                ChangeRequestListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK)
                .body(ChangeRequestListResponse.builder().list(list).totalCnt(list.size()).build());
    }

    /** 변경 요청 단건 상세(확인/반려 팝업용). */
    @GetMapping("/change-requests/{changeReqId}")
    public ResponseEntity<?> getChangeRequestDetail(
            @PathVariable("changeReqId") String changeReqId,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        LeaveChangeRequestRowResult detail = attd13Service.getChangeRequestDetail(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_authCd(), tokenInfo.gv_userCd(), changeReqId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ChangeRequestDetailResponse.builder().detail(detail).build());
    }

    /** 관리자 발의(이동/삭제). 사유 필수. */
    @PostMapping("/change-requests")
    public ResponseEntity<?> createChangeRequest(
            @RequestBody @Valid ChangeRequestCreateRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd13Service.createChangeRequest(
                ChangeRequestCreateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 관리자 최종 확인(동의된 요청만 반영). */
    @PostMapping("/change-requests/{changeReqId}/confirm")
    public ResponseEntity<?> confirmChangeRequest(
            @PathVariable("changeReqId") String changeReqId,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd13Service.confirmChangeRequest(
                ChangeRequestConfirmParam.from(changeReqId, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 관리자 반려(작업2). 주로 WORKER 발의(AGREED)건. 사유 필수. 원 연차 불변. */
    @PostMapping("/change-requests/{changeReqId}/reject")
    public ResponseEntity<?> rejectChangeRequest(
            @PathVariable("changeReqId") String changeReqId,
            @RequestBody @Valid ChangeRequestRejectRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorization) {

        attd13Service.rejectChangeRequest(
                ChangeRequestRejectParam.from(changeReqId, request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
