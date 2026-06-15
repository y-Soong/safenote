package com.prafta.app.safety.history.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.safety.history.application.param.MySafetyHistoryParam;
import com.prafta.app.safety.history.dto.response.MySafetyHistoryResponse;
import com.prafta.app.safety.history.service.AppMySafetyHistoryService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 내 안전활동 이력 컨트롤러 (prafta-app-025 J1-10 B-6).
 *
 * <p>최종 URL(ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 * <ul>
 *   <li>GET /prafta/appApi/safety/history/my  (본인 점검 + 위험성 이력 합본, 시간순)</li>
 * </ul>
 * <p>프론트 호출 = /appApi/safety/history/my
 *
 * <p>진입: MainView 안전 활동 카드 ">"(onSafetyDetail) → /MySafetyHistory 화면 → 본 EP.
 *    하단 "안전" 탭(/SafetyHub) 과 역할 분리(허브 vs 본인 이력 조회).
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. 식별자(cmpnyCd/userCd/siteCd)는 JWT 클레임에서만 도출하며,
 *    본인 이력 필터(INSERT_NO/INIT_ASSESSOR_ID = userCd)를 서버가 강제한다(다른 USER_CD 수신 경로 없음).
 *    역할 게이트 불요(본인 데이터만 반환). 정규직/일용직 모두 본인 이력 허용.
 */
@Slf4j
@RestController
@RequestMapping("/safety/history")
@RequiredArgsConstructor
public class AppMySafetyHistoryController {

    private final AppMySafetyHistoryService appMySafetyHistoryService;
    private final JwtUtil jwtUtil;

    /** 본인 안전활동 이력 합본 조회(kind/page/pageSize 선택). */
    @GetMapping("/my")
    public ResponseEntity<?> getMyHistory(
            @RequestParam(value = "kind", required = false) String kind,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        MySafetyHistoryResponse response = appMySafetyHistoryService.selectMyHistory(
                MySafetyHistoryParam.of(kind, page, pageSize, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
