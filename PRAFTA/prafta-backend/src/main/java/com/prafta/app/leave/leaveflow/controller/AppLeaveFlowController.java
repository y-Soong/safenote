package com.prafta.app.leave.leaveflow.controller;

import java.util.Map;

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
import com.prafta.app.leave.leaveflow.application.param.LeaveDayScheduleParam;
import com.prafta.app.leave.leaveflow.application.param.LeaveDeductionPreviewParam;
import com.prafta.app.leave.leaveflow.dto.request.LeaveApplyMultiRequest;
import com.prafta.app.leave.leaveflow.dto.request.LeaveApplyRequest;
import com.prafta.app.leave.leaveflow.dto.request.LeaveDeductionPreviewRequest;
import com.prafta.app.leave.leaveflow.dto.response.ApprovalPresetListResponse;
import com.prafta.app.leave.leaveflow.dto.response.ApproverSearchResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMetaResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveDayScheduleResponse;
import com.prafta.app.leave.leaveflow.dto.response.LeaveDeductionPreviewResponse;
import com.prafta.app.leave.leaveflow.service.AppLeaveFlowService;
import com.prafta.app.leave.leaveflow.service.MultiDayLeaveApplyService;
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
    /** prafta-leavemulti: 기간(From-To) 신청 오케스트레이터(별도 빈 — self-invocation 함정 회피). */
    private final MultiDayLeaveApplyService multiDayLeaveApplyService;
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
     * 신청 대상일 근무/휴게 시각 조회(조회 전용) — 시간차 연차 휴게 가로지름(ATTD_400_055) 사전 안내용.
     * 식별값은 토큰 강제(IDOR 차단), workYmd 만 쿼리로 받는다. 스케줄 없는 날은 hasSchedule=false.
     */
    @GetMapping("/day-schedule")
    public ResponseEntity<?> getDaySchedule(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "workYmd", required = false) String workYmd
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        LeaveDayScheduleResponse response = appLeaveFlowService.selectDaySchedule(
                LeaveDayScheduleParam.from(tokenInfo, workYmd));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * LC-07(T3): 예상 차감액 미리보기 — INSERT 없음(조회 전용, 웹 /leaveflow/preview-deduction 미러).
     * 검증 가드는 신청과 동일하게 태우고(위반 시 해당 에러 그대로), 잔여 부족은 플래그로 응답한다.
     * 인가: 본인 신청 기준(토큰 gv_userCd)만. 일용직은 연차 비대상이라 신청과 동일하게 차단.
     */
    @PostMapping("/preview-deduction")
    public ResponseEntity<?> previewDeduction(
            @Valid @RequestBody LeaveDeductionPreviewRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        // 신청(/apply)과 동일 게이트 — 일용직 서버 차단(prafta-app-027 follow-up 미러).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);

        LeaveDeductionPreviewResponse response = appLeaveFlowService.previewDeduction(
                LeaveDeductionPreviewParam.from(request, tokenInfo));

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

    // ============================================================
    // prafta-leavemulti: 연차 기간(From-To) 신청 — 종일 전용
    // ============================================================

    /**
     * 기간 신청 미리보기 — 구간의 날짜별 선택 가능 여부·기본 체크 상태·잔여 배정 결과.
     *
     * <p>화면은 범위(From-To) 확정 직후 <b>1회</b> 호출한다(체크 토글마다 재호출하지 않음 — 로컬 처리).
     * 식별값(cmpny/site/user)은 토큰에서만 도출한다(IDOR).
     */
    @GetMapping("/apply-multi-preview")
    public ResponseEntity<?> applyMultiPreview(
            @RequestParam("leaveCd") String leaveCd,
            @RequestParam("fromYmd") String fromYmd,
            @RequestParam("toYmd") String toYmd,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);

        return ResponseEntity.status(HttpStatus.OK)
                .body(multiDayLeaveApplyService.preview(tokenInfo, leaveCd, fromYmd, toYmd));
    }

    /**
     * 기간 신청 제출 — 선택된 날짜 목록을 날짜별 단일일 신청 N건으로 분해한다.
     *
     * <p>정책 ②에 따라 <b>하나라도 막히면 전체 실패</b>하며, 막힌 날짜 전부를 응답의
     * {@code blockedDates} 로 함께 돌려준다(첫 실패에서 끊으면 "고침 → 재제출"이 반복된다).
     */
    @PostMapping("/apply-multi")
    public ResponseEntity<?> applyLeaveMulti(
            @Valid @RequestBody LeaveApplyMultiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        // 단건 신청과 동일 — 일용직은 연차 신청 비해당(서버 차단).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);

        String groupId = multiDayLeaveApplyService.applyMulti(
                tokenInfo, request.getLeaveCd(), request.getLeaveType(), request.getDates(),
                request.getReason(), request.getApproverUserCds(), request.getPresetId());

        return ResponseEntity.status(HttpStatus.OK).body(Map.of("groupId", groupId));
    }
}
