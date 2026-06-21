package com.prafta.app.req.req07.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.req.req07.application.param.AttdCorrectionParam;
import com.prafta.app.req.req07.application.param.OvertimeParam;
import com.prafta.app.req.req07.application.param.SchedModifyParam;
import com.prafta.app.req.req07.dto.request.AttdCorrectionRequest;
import com.prafta.app.req.req07.dto.request.OvertimeRequest;
import com.prafta.app.req.req07.dto.request.SchedModifyRequest;
import com.prafta.app.req.req07.dto.response.AppliedOvertimeResponse;
import com.prafta.app.req.req07.dto.response.RegisterReqResponse;
import com.prafta.app.req.req07.dto.response.SchedOptionResponse;
import com.prafta.app.req.req07.service.AppReq07Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.EmploymentTypeGuard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-007: 모바일 앱 근태 요청 폼 3종 (스케줄 수정 / 근태 보정 / 초과근무) 등록 컨트롤러.
 *
 * <p>실제 매핑 경로 (자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 * <ul>
 *   <li>{@code POST /prafta/appApi/req07/sched-modify}</li>
 *   <li>{@code POST /prafta/appApi/req07/attd-correction}</li>
 *   <li>{@code POST /prafta/appApi/req07/overtime}</li>
 * </ul>
 *
 * <p>인증/식별: AuthAspect 가 JWT 를 검증, 본 컨트롤러는 {@link JwtUtil#getAllClaimsAsMap}
 * 으로 TokenInfo 를 도출한다. 식별값(cmpnyCd/siteCd/userCd) 을 바디로 받지 않는다 (IDOR 가드).
 */
@Slf4j
@RestController
@RequestMapping("/req07")
@RequiredArgsConstructor
public class AppReq07Controller {

    private final AppReq07Service appReq07Service;
    private final JwtUtil jwtUtil;

    /**
     * 스케줄 선택 옵션 목록 조회 (prafta-app-007 F2).
     *
     * <p>실제 매핑: {@code GET /prafta/appApi/req07/schedules}.
     * 식별값(cmpnyCd/siteCd)은 JWT(TokenInfo)에서만 도출한다 — 바디/쿼리 미수신 (IDOR 가드).
     */
    @GetMapping("/schedules")
    public ResponseEntity<SchedOptionResponse> getSchedules(
            @RequestParam(value = "workYmd", required = false) String workYmd,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || !StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_siteCd())
                || !StringUtils.hasText(tokenInfo.gv_userCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        // prafta-com-008-E-9a: userDefaultSchCd 동반 위해 userCd(JWT) 전달(IDOR — 본인만).
        // prafta-com-008-D-5: workYmd(대상 일자, optional) 전달 — 교대 소속 구간이면 응답 shiftLocked=true.
        //   식별값(cmpnyCd/siteCd/userCd)은 JWT 도출만 사용(본인 한정 IDOR). workYmd 만 쿼리로 수신(본인 일자 판정용).
        SchedOptionResponse response = appReq07Service.getSchedOptions(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_siteCd(), tokenInfo.gv_userCd(), workYmd);
        return ResponseEntity.ok(response);
    }

    /**
     * 이미 등록(적용)된 초과근무 목록 조회 (prafta-app-030).
     *
     * <p>실제 매핑: {@code GET /prafta/appApi/req07/applied-overtimes?workYmd=YYYYMMDD}.
     * 초과근무 신청 폼에서 중복등록 방지를 위해 기존 적용 OT(TB_USER_OVERTIME_MGMT)를 표시/대조한다.
     * 식별값(cmpnyCd/siteCd/userCd)은 JWT(TokenInfo)에서만 도출한다 — 바디/쿼리 미수신(IDOR 가드).
     * workYmd 는 본인 일자 판정용 쿼리 파라미터로만 수신하며 형식(YYYYMMDD 8자리 숫자)을 검증한다.
     */
    @GetMapping("/applied-overtimes")
    public ResponseEntity<AppliedOvertimeResponse> getAppliedOvertimes(
            @RequestParam(value = "workYmd", required = false) String workYmd,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || !StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_siteCd())
                || !StringUtils.hasText(tokenInfo.gv_userCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        if (!isValidYmd(workYmd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        AppliedOvertimeResponse response = appReq07Service.getAppliedOvertimes(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_siteCd(), tokenInfo.gv_userCd(), workYmd);
        return ResponseEntity.ok(response);
    }

    /** workYmd 형식 검증(YYYYMMDD 8자리 숫자). 형식 위반/누락이면 false. */
    private static boolean isValidYmd(String ymd) {
        if (ymd == null || ymd.length() != 8) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            char c = ymd.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * 스케줄 수정 요청 등록 (REQ_TYPE='10').
     */
    @PostMapping("/sched-modify")
    public ResponseEntity<RegisterReqResponse> registerSchedModify(
            @RequestBody SchedModifyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        // prafta-app-027 follow-up: 일용직은 스케줄 수정 요청 비해당 → 서버 차단(J1-4 서버측 보강).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);
        RegisterReqResponse response = appReq07Service.registerSchedModify(
                SchedModifyParam.from(request, tokenInfo)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 근태 보정 요청 등록 (REQ_TYPE 자동 분기 — '01' or '02' or 'MIXED').
     */
    @PostMapping("/attd-correction")
    public ResponseEntity<RegisterReqResponse> registerAttdCorrection(
            @RequestBody AttdCorrectionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        // prafta-app-027 follow-up: 일용직은 근태 보정 요청 비해당 → 서버 차단(J1-4 서버측 보강).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);
        RegisterReqResponse response = appReq07Service.registerAttdCorrection(
                AttdCorrectionParam.from(request, tokenInfo)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 초과근무 신청 등록 (REQ_TYPE='03').
     */
    @PostMapping("/overtime")
    public ResponseEntity<RegisterReqResponse> registerOvertime(
            @RequestBody OvertimeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        // prafta-app-027 follow-up: 일용직은 초과근무 신청 비해당 → 서버 차단(J1-4 서버측 보강).
        EmploymentTypeGuard.assertNotDailyWorker(tokenInfo);
        RegisterReqResponse response = appReq07Service.registerOvertime(
                OvertimeParam.from(request, tokenInfo)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
