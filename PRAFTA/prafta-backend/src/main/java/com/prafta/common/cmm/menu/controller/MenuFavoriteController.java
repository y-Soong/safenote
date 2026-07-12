package com.prafta.common.cmm.menu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.cmm.menu.application.param.MenuFavoriteListParam;
import com.prafta.common.cmm.menu.application.param.MenuFavoriteToggleParam;
import com.prafta.common.cmm.menu.dto.request.MenuFavoriteToggleRequest;
import com.prafta.common.cmm.menu.dto.response.MenuFavoriteListResponse;
import com.prafta.common.cmm.menu.dto.response.MenuFavoriteToggleResponse;
import com.prafta.common.cmm.menu.service.MenuFavoriteService;
import com.prafta.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * LNB 즐겨찾기 컨트롤러.
 *
 * <p>com.prafta.common 패키지 → ApiPrefixConfig 가 "/prafta/comApi" 프리픽스를 자동 부여한다.
 *   따라서 실제 매핑은 {@code /prafta/comApi/menu/favorites},
 *   {@code /prafta/comApi/menu/favorite-toggle} 이다.
 *
 * <p>USER_CD/CMPNY_CD 는 JWT 도출값만 신뢰한다(클라 입력 금지 — IDOR 방지).
 */
@Slf4j
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuFavoriteController {

	private final MenuFavoriteService menuFavoriteService;
	private final JwtUtil jwtUtil;

	/* 내 즐겨찾기 MENU_D_ID 목록 조회 */
	@GetMapping("/favorites")
	public ResponseEntity<?> getMyFavorites(
			@RequestHeader(value = "Authorization", required = true) String authorization) {

		MenuFavoriteListResponse response = menuFavoriteService.selectMyFavorites(
				MenuFavoriteListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/* 즐겨찾기 토글(추가/해제) */
	@PostMapping("/favorite-toggle")
	public ResponseEntity<?> toggleFavorite(
			@RequestBody MenuFavoriteToggleRequest request,
			@RequestHeader(value = "Authorization", required = true) String authorization) {

		MenuFavoriteToggleResponse response = menuFavoriteService.toggleFavorite(
				MenuFavoriteToggleParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
