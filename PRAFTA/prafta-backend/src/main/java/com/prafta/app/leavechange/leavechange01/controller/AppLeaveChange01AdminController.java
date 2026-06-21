package com.prafta.app.leavechange.leavechange01.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.leavechange.leavechange01.dto.response.PendingConfirmListResponse;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd13.application.param.ChangeRequestConfirmParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestListParam;
import com.prafta.web.attd.attd13.application.param.ChangeRequestRejectParam;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestListRequest;
import com.prafta.web.attd.attd13.dto.request.ChangeRequestRejectRequest;
import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;
import com.prafta.web.attd.attd13.service.Attd13Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 변경/삭제 — 관리자(앱) 최종 확인 컨트롤러 (PRAFTA-COM-008-C, 앱 미러).
 *
 * <p>웹 Attd_13(연차 변경 동의 관리)의 앱 대응. 근로자가 동의(AGREED)한 요청을 관리자가
 *   앱에서도 최종 확인(CONFIRMED, 실제 반영) 또는 반려(REJECTED)할 수 있게 한다.
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/leavechange/admin/pending-confirms  (관리자 스코프 내 AGREED 목록)</li>
 *   <li>POST /prafta/appApi/leavechange/admin/{id}/confirm       (최종 확인 — 실제 반영)</li>
 *   <li>POST /prafta/appApi/leavechange/admin/{id}/reject        (반려, 사유 필수)</li>
 * </ul>
 *
 * <p>권한/IDOR: 식별자(회사/권한/사용자/사업장/노드)는 JWT 클레임에서만 도출한다(path/body 불신뢰).
 *   스코프/권한 검증은 웹과 동일하게 공유 {@link Attd13Service} 가 수행한다
 *   (master/hr=전사, 노드 관리자=담당 노드+하위, safe 제외 — 비관리자는 fail-closed 403).
 */
@Slf4j
@RestController
@RequestMapping("/leavechange/admin")
@RequiredArgsConstructor
public class AppLeaveChange01AdminController {

    private static final String STATUS_AGREED = "AGREED";

    private final Attd13Service attd13Service;
    private final JwtUtil jwtUtil;

    /**
     * 관리자 스코프 내 확인 대기(AGREED) 요청 목록.
     *
     * <p>스코프는 웹 Attd_13 과 동일: master/hr 는 전사(노드 미지정), 노드 관리자는 본인 담당 노드(+하위)로
     *   강제한다. 비관리자(일반 근로자)는 서비스의 스코프 가드에서 403(fail-closed).
     */
    @GetMapping("/pending-confirms")
    public ResponseEntity<?> getPendingConfirms(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);

        // AGREED(확인 대기) 만 조회. 노드 관리자는 본인 노드(+하위)로 스코프 강제(웹과 동일 단일출처).
        ChangeRequestListRequest req = new ChangeRequestListRequest();
        req.setREQ_STATUS(STATUS_AGREED);
        req.setUSER_NM(keyword);
        if (!AuthRoleUtils.isManager(token.gv_authCd())) {
            req.setNODE_CD(token.gv_nodeCd());
            req.setINC_SUB_NODE_YN("Y");
        }

        List<LeaveChangeRequestRowResult> list =
                attd13Service.getChangeRequests(ChangeRequestListParam.from(req, token));

        return ResponseEntity.status(HttpStatus.OK)
                .body(PendingConfirmListResponse.builder().list(list).totalCnt(list.size()).build());
    }

    /** 관리자 최종 확인(AGREED → CONFIRMED, 실제 반영). 대상은 path 로만 지정. */
    @PostMapping("/{changeReqId}/confirm")
    public ResponseEntity<?> confirm(
            @PathVariable("changeReqId") String changeReqId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        attd13Service.confirmChangeRequest(ChangeRequestConfirmParam.from(changeReqId, token));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 관리자 반려(AGREED → REJECTED, 원 연차 불변). 사유 필수. */
    @PostMapping("/{changeReqId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable("changeReqId") String changeReqId,
            @RequestBody @Valid ChangeRequestRejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        attd13Service.rejectChangeRequest(ChangeRequestRejectParam.from(changeReqId, request, token));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
