package com.prafta.app.leave.leave01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import com.prafta.app.leave.leave01.application.param.MyLeaveSummaryParam;
import com.prafta.app.leave.leave01.application.param.MyLeaveUseListParam;
import com.prafta.app.leave.leave01.dto.response.MyLeaveSummaryResponse;
import com.prafta.app.leave.leave01.dto.response.MyLeaveUseListResponse;
import com.prafta.app.leave.leave01.service.AppLeave01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-005: 앱 "연차 현황"(본인 잔여연차 상세) 컨트롤러.
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 *   GET /prafta/appApi/leave01/my-leave-summary
 * <p>인증/식별: AuthAspect 가 JWT 를 검증하고, 본 컨트롤러는
 *   jwtUtil.getAllClaimsAsMap(Authorization) → TokenInfo 로 cmpnyCd/userCd 를 도출한다.
 *   userCd 등 식별값을 쿼리/바디로 받지 않는다(본인 자기조회만, AppHome01Controller 패턴 동일).
 */
@Slf4j
@RestController
@RequestMapping("/leave01")
@RequiredArgsConstructor
public class AppLeave01Controller {

    private final AppLeave01Service appLeave01Service;
    private final JwtUtil jwtUtil;

    /**
     * 본인 연차 현황 조회(그룹 3종 + 소멸임박 + 사용자 메타 단일 응답).
     */
    @GetMapping("/my-leave-summary")
    public ResponseEntity<?> getMyLeaveSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        MyLeaveSummaryResponse response = appLeave01Service.selectMyLeaveSummary(
                MyLeaveSummaryParam.from(tokenInfo)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 본인 연차 사용 내역(연 단위) 조회 — 연차 현황 화면 하단 리스트.
     * <p>GET /prafta/appApi/leave01/my-leave-uses?year=YYYY (year 미지정=올해)
     */
    @GetMapping("/my-leave-uses")
    public ResponseEntity<?> getMyLeaveUses(
            @RequestParam(value = "year", required = false) String year,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        MyLeaveUseListResponse response = appLeave01Service.selectMyLeaveUses(
                MyLeaveUseListParam.from(tokenInfo, year)
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
