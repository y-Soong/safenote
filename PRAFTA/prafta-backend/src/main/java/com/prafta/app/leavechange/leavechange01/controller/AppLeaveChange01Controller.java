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
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.leavechange.leavechange01.application.param.LeaveChangeMoveParam;
import com.prafta.app.leavechange.leavechange01.application.param.LeaveChangeRespondParam;
import com.prafta.app.leavechange.leavechange01.dto.request.LeaveChangeMoveRequest;
import com.prafta.app.leavechange.leavechange01.dto.request.LeaveChangeRespondRequest;
import com.prafta.app.leavechange.leavechange01.dto.response.MovableLeaveListResponse;
import com.prafta.app.leavechange.leavechange01.dto.response.PendingConsentListResponse;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.EmploymentTypeGuard;
import com.prafta.web.attd.attd13.result.LeaveChangeRequestRowResult;
import com.prafta.web.attd.attd13.result.MovableLeaveResult;
import com.prafta.web.attd.attd13.service.Attd13Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 연차 변경/삭제 동의·거부 — 근로자(앱) 컨트롤러 (PRAFTA-COM-008-C).
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/leavechange/pending-consents   (본인 대기 응답 대상)</li>
 *   <li>POST /prafta/appApi/leavechange/{id}/respond        (동의/거부)</li>
 *   <li>GET  /prafta/appApi/leavechange/movable-leaves      (본인 이동가능 연차)</li>
 *   <li>POST /prafta/appApi/leavechange/move-requests       (근로자 이동 발의, 취소 불가)</li>
 * </ul>
 * 식별값(회사/사용자)은 JWT 에서만 도출(IDOR 차단). 비즈니스 로직은 공유 {@link Attd13Service} 위임.
 */
@Slf4j
@RestController
@RequestMapping("/leavechange")
@RequiredArgsConstructor
public class AppLeaveChange01Controller {

    private final Attd13Service attd13Service;
    private final JwtUtil jwtUtil;

    /** 본인 대상 대기(REQUESTED) 응답 대상 요청 목록. */
    @GetMapping("/pending-consents")
    public ResponseEntity<?> getPendingConsents(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        List<LeaveChangeRequestRowResult> list =
                attd13Service.getPendingConsents(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK)
                .body(PendingConsentListResponse.builder().list(list).totalCnt(list.size()).build());
    }

    /** 근로자 응답(동의/거부). 거부 시 사유 필수. */
    @PostMapping("/{changeReqId}/respond")
    public ResponseEntity<?> respond(
            @PathVariable("changeReqId") String changeReqId,
            @RequestBody @Valid LeaveChangeRespondRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        LeaveChangeRespondParam param = LeaveChangeRespondParam.from(changeReqId, request, tokenInfo);

        attd13Service.respondChangeRequest(
                param.gvCmpnyCd(), param.gvUserCd(), param.changeReqId(),
                param.workerResponse(), param.responseReason());

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /** 본인 이동 가능 연차일 목록(C-5a). */
    @GetMapping("/movable-leaves")
    public ResponseEntity<?> getMovableLeaves(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        List<MovableLeaveResult> list =
                attd13Service.getMovableLeaves(tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd());

        return ResponseEntity.status(HttpStatus.OK)
                .body(MovableLeaveListResponse.builder().list(list).totalCnt(list.size()).build());
    }

    /** 근로자 이동 발의(MOVE 전용, 취소 불가, C-5a). */
    @PostMapping("/move-requests")
    public ResponseEntity<?> createMoveRequest(
            @RequestBody @Valid LeaveChangeMoveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        // prafta-app-027 follow-up: 일용직은 연차 이동 발의(연차 관련 쓰기) 비해당 → 서버 차단(J1-4 서버측 보강).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);
        LeaveChangeMoveParam param = LeaveChangeMoveParam.from(request, tokenInfo);

        attd13Service.createWorkerMoveRequest(
                param.gvCmpnyCd(), param.gvUserCd(), param.targetLeaveId(),
                param.moveTargetDate(), param.reqReason());

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
