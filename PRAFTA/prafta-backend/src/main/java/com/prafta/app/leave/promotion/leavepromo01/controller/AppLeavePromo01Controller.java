package com.prafta.app.leave.promotion.leavepromo01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.leave.promotion.leavepromo01.application.param.PromotionPlanParam;
import com.prafta.app.leave.promotion.leavepromo01.dto.request.PromotionPlanRequest;
import com.prafta.app.leave.promotion.leavepromo01.dto.response.PromotionActiveResponse;
import com.prafta.app.leave.promotion.leavepromo01.dto.response.PromotionPlanResultResponse;
import com.prafta.app.leave.promotion.leavepromo01.service.AppLeavePromo01Service;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-008-A-3: 앱 1차 연차 사용촉진 계획서 컨트롤러(앱 완전 분리).
 *
 * <p>실제 매핑 경로(자동 프리픽스 com.prafta.app.* → /prafta/appApi):
 * <ul>
 *   <li>GET  /prafta/appApi/leavepromo01/active   — 진행 중 1차 촉진 컨텍스트</li>
 *   <li>POST /prafta/appApi/leavepromo01/plan     — 선택 날짜 다건 1일 단위 등록</li>
 *   <li>POST /prafta/appApi/leavepromo01/notified — 로그인 안내 1회 노출 완료 플래그</li>
 * </ul>
 * 인증/식별: AuthAspect 가 JWT 검증, 본 컨트롤러는 jwtUtil.getAllClaimsAsMap(Authorization) → TokenInfo
 *   로 cmpny/site/user 를 도출한다. 식별값을 쿼리/바디로 받지 않는다(본인 자기처리만, IDOR 차단).
 */
@Slf4j
@RestController
@RequestMapping("/leavepromo01")
@RequiredArgsConstructor
public class AppLeavePromo01Controller {

    private final AppLeavePromo01Service appLeavePromo01Service;
    private final JwtUtil jwtUtil;

    /** 진행 중 1차 촉진 컨텍스트(없으면 inProgress=false). */
    @GetMapping("/active")
    public ResponseEntity<?> getActive(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        PromotionActiveResponse response = appLeavePromo01Service.getActiveContext(tokenInfo);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 선택 날짜 다건 1일 단위 촉진(1차/자발) 연차 등록. 일부/미제출 허용. */
    @PostMapping("/plan")
    public ResponseEntity<?> submitPlan(
            @RequestBody PromotionPlanRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        PromotionPlanResultResponse response = appLeavePromo01Service.submitPlan(
                PromotionPlanParam.from(request, tokenInfo));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 로그인 안내 1회 노출 완료(LOGIN_NOTIFIED_YN='Y'). */
    @PostMapping("/notified")
    public ResponseEntity<?> markNotified(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        appLeavePromo01Service.markLoginNotified(tokenInfo);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
