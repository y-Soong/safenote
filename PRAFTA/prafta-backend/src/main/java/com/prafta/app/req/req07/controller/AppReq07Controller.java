package com.prafta.app.req.req07.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.req.req07.application.param.AttdCorrectionParam;
import com.prafta.app.req.req07.application.param.OvertimeParam;
import com.prafta.app.req.req07.application.param.SchedModifyParam;
import com.prafta.app.req.req07.dto.request.AttdCorrectionRequest;
import com.prafta.app.req.req07.dto.request.OvertimeRequest;
import com.prafta.app.req.req07.dto.request.SchedModifyRequest;
import com.prafta.app.req.req07.dto.response.RegisterReqResponse;
import com.prafta.app.req.req07.dto.response.SchedOptionResponse;
import com.prafta.app.req.req07.service.AppReq07Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;

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
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null
                || !StringUtils.hasText(tokenInfo.gv_cmpnyCd())
                || !StringUtils.hasText(tokenInfo.gv_siteCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        SchedOptionResponse response = appReq07Service.getSchedOptions(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_siteCd());
        return ResponseEntity.ok(response);
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
        RegisterReqResponse response = appReq07Service.registerOvertime(
                OvertimeParam.from(request, tokenInfo)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
