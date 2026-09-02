package com.prafta.web.location.location01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.web.location.location01.application.param.LocationConsentStatusParam;
import com.prafta.web.location.location01.service.Location01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 위치정보 동의 현황(Location_01) 컨트롤러 — 위치정보 동의철회·중지 S5.
 *
 * <p>자동 프리픽스(com.prafta.web.* → /prafta/webApi) 적용 → 실제 매핑:
 * <ul>
 *   <li>GET /prafta/webApi/location01/consent-status-lists — 사용자별 동의 상태 목록</li>
 *   <li>GET /prafta/webApi/location01/consent-histories    — 특정 사용자 전이/파기 이력</li>
 * </ul>
 *
 * <p>★조회 범위를 결정하는 값(cmpnyCd/authCd/nodeCd)은 전부 JWT 클레임에서만 도출한다.
 * 요청으로 받는 것은 대상 사업장·필터·대상 사용자뿐이고, 그 셋도 서비스가 인가로 검증한다.
 *
 * <p>★조회 전용이다 — 관리자가 타인의 동의 상태를 바꾸는 EP 는 만들지 않는다.
 * 철회는 되돌릴 수 없는 파기를 동반하므로 본인만 수행한다.
 */
@Slf4j
@RestController
@RequestMapping("/location01")
@RequiredArgsConstructor
public class Location01Controller {

    private final Location01Service location01Service;
    private final JwtUtil jwtUtil;

    /**
     * 위치정보 동의 현황 목록.
     *
     * @param stateFilter {@code ISSUE} = 동의 상태가 아닌 사람만(기본 화면), 그 외/미전달 = 전체
     */
    @GetMapping("/consent-status-lists")
    public ResponseEntity<?> getConsentStatusList(
            @RequestParam("siteCd") String siteCd,
            @RequestParam(value = "stateFilter", required = false) String stateFilter,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                location01Service.selectConsentStatusList(
                        LocationConsentStatusParam.of(siteCd, stateFilter, resolveToken(authorization))));
    }

    /** 특정 사용자의 동의 전이 이력 + 파기 이력. 대상 범위 검증은 서비스가 수행한다. */
    @GetMapping("/consent-histories")
    public ResponseEntity<?> getConsentHistories(
            @RequestParam("siteCd") String siteCd,
            @RequestParam("userCd") String userCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        return ResponseEntity.status(HttpStatus.OK).body(
                location01Service.selectConsentHistories(
                        LocationConsentStatusParam.of(siteCd, null, resolveToken(authorization)), userCd));
    }

    /** JWT 클레임 → TokenInfo. userCd 부재면 인증 결함. */
    private TokenInfo resolveToken(String authorization) {
        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        return tokenInfo;
    }
}
