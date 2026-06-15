package com.prafta.app.leave.leaveflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.leave.leaveflow.application.param.LeaveApplyMetaParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApplyParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveApproverSearchParam;
import com.prafta.app.leave.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.app.leave.leaveflow.dto.response.ApprovalPresetListResponse;
import com.prafta.app.leave.leaveflow.dto.response.ApproverSearchResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMetaResponse;
import com.prafta.app.leave.leaveflow.service.AppLeaveFlowService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.EmploymentTypeGuard;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-018-A: 앱 연차 신청 폼 메타 조회 컨트롤러(읽기 전용).
 *
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 * <ul>
 *   <li>GET /prafta/appApi/leaveflow/apply-meta</li>
 *   <li>GET /prafta/appApi/leaveflow/approval-presets</li>
 *   <li>GET /prafta/appApi/leaveflow/approver-search?keyword=&page=&size=</li>
 * </ul>
 * 인증/식별: AuthAspect 가 JWT 를 검증하고, 본 컨트롤러는 jwtUtil.getAllClaimsAsMap(Authorization)
 *   → TokenInfo 로 cmpnyCd/siteCd/userCd 를 도출한다. 식별값을 쿼리/바디로 받지 않는다(IDOR 차단).
 *   approver-search 의 keyword/page/size 만 쿼리스트링으로 받는다(사업장 스코프는 토큰 강제).
 */
@Slf4j
@RestController
@RequestMapping("/leaveflow")
@RequiredArgsConstructor
public class AppLeaveFlowController {

    private final AppLeaveFlowService appLeaveFlowService;
    private final JwtUtil jwtUtil;

    /** 신청 가능 연차종류 + 허용 사용단위 + 잔여 메타. */
    @GetMapping("/apply-meta")
    public ResponseEntity<?> getApplyMeta(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        LeaveApplyMetaResponse response = appLeaveFlowService.selectApplyMeta(
                LeaveApplyMetaParam.from(tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 본인 소유 결재선 프리셋 목록(mypage01 재사용). */
    @GetMapping("/approval-presets")
    public ResponseEntity<?> getApprovalPresets(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        ApprovalPresetListResponse response = appLeaveFlowService.selectApprovalPresets(
                LeaveApplyMetaParam.from(tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 결재자 후보 검색(사업장 스코프, PII 최소노출, LIMIT/페이징). */
    @GetMapping("/approver-search")
    public ResponseEntity<?> searchApprovers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        ApproverSearchResponse response = appLeaveFlowService.searchApprovers(
                LeaveApproverSearchParam.from(tokenInfo, keyword, page, size)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * prafta-app-018-B: 연차 신청 1건 처리(요청 INSERT + 결재선 + 사용기록 + 부여 재계산).
     *
     * <p>식별값(cmpny/site/user)은 토큰에서만 강제(IDOR). 본문은 leaveCd/workYmd/useUnitType/시각/사유/
     *   approverUserCds/presetId 만 신뢰하며, 단위 게이팅·구조검증·사후마감·잔여검증을 모두 통과한 후 INSERT 한다.
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(
            @Valid @RequestBody LeaveApplyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        // prafta-app-027 follow-up: 일용직은 연차 신청 비해당 → 서버 차단(표시숨김 J1-4의 서버측 보강).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);

        appLeaveFlowService.submitLeave(LeaveApplyParam.from(request, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
