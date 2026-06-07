package com.prafta.app.admin.access.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.admin.access.application.param.AdminAccessParam;
import com.prafta.app.admin.access.dto.response.AdminAccessContextResponse;
import com.prafta.app.admin.access.service.AppAdminAccessService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 001-P1-B1: 모바일 앱 관리자 모드 진입판정 컨트롤러.
 *
 * <p>최종 URL (ApiPrefixConfig 가 com.prafta.app.* 에 /prafta/appApi 자동 부여):
 *   <ul>
 *     <li>GET /prafta/appApi/admin/access-context           (토큰 사업장 기준 진입판정)</li>
 *     <li>GET /prafta/appApi/admin/access-context?siteCd=... (현장전환 후 재조회, 서버가 USE_YN='Y' 검증 — D5)</li>
 *   </ul>
 *
 * <p>인증/IDOR: AuthAspect 가 JWT 를 검증한다. cmpnyCd/userCd/authCd/siteCd 는 JWT 클레임에서만 도출하며
 *   바디/패스로 식별자를 받지 않는다(D1). 유일한 입력 siteCd(쿼리)는 현장전환 재조회용이며 서버가 접근권한을 검증한다.
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AppAdminAccessController {

    private final AppAdminAccessService appAdminAccessService;
    private final JwtUtil jwtUtil;

    /** A1: 관리자 모드 진입판정. */
    @GetMapping("/access-context")
    public ResponseEntity<?> getAccessContext(
            @RequestParam(value = "siteCd", required = false) String siteCd
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        AdminAccessContextResponse response = appAdminAccessService.selectAccessContext(
                AdminAccessParam.from(siteCd, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
