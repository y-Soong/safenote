package com.prafta.web.tbm.tbm02.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.tbm.tbm02.application.param.OptionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionCancelParam;
import com.prafta.web.tbm.tbm02.application.param.SessionDetailParam;
import com.prafta.web.tbm.tbm02.application.param.SessionListParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPwdParam;
import com.prafta.web.tbm.tbm02.application.param.SessionSaveParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;
import com.prafta.web.tbm.tbm02.dto.request.OptionRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionCancelRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionDetailRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionListRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionPwdRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionSaveRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionUpdateRequest;
import com.prafta.web.tbm.tbm02.dto.response.ContentOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.RiskOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionDetailResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionSaveResponse;
import com.prafta.web.tbm.tbm02.dto.response.SiteOptionResponse;
import com.prafta.web.tbm.tbm02.service.Tbm02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 세션 관리(W-04~06). prafta-033-B.
 *
 * <p>경계: 세션은 OPENED까지만 다룬다. 교육 시작(IN_PROGRESS 전이)/동기화/종료/QR출결은
 * C 단계(tbm03) 소관이므로 본 컨트롤러에 없다.
 */
@Slf4j
@RestController
@RequestMapping("/tbm02")
@RequiredArgsConstructor
public class Tbm02Controller {

	private final Tbm02Service tbm02Service;
	private final JwtUtil jwtUtil;

	/** W-04 세션 목록(필터/페이징/집계). */
	@GetMapping("/sessions")
	public ResponseEntity<?> getSessions(@ModelAttribute SessionListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionListResponse response = tbm02Service.selectSessionList(
				SessionListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-06 세션 상세(+콘텐츠/위험성평가 매핑). */
	@GetMapping("/session-detail")
	public ResponseEntity<?> getSessionDetail(@ModelAttribute SessionDetailRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionDetailResponse response = tbm02Service.selectSessionDetail(
				SessionDetailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-05 개설(OPENED) / 임시저장(DRAFT). */
	@PostMapping(value = "/save-session", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> saveSession(@RequestBody SessionSaveRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionSaveResponse response = tbm02Service.saveSession(
				SessionSaveParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-06 수정(DRAFT/OPENED만). */
	@PostMapping(value = "/update-session", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateSession(@RequestBody SessionUpdateRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.updateSession(
				SessionUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	/** W-06 취소(DRAFT/OPENED만, CANCEL_REASON 필수). */
	@PostMapping(value = "/cancel-session", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> cancelSession(@RequestBody SessionCancelRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.cancelSession(
				SessionCancelParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	/** W-06 비밀번호 재발급(OPENED만). */
	@PostMapping(value = "/regenerate-passwords", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> regeneratePasswords(@RequestBody SessionPwdRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionPwdResponse response = tbm02Service.regeneratePasswords(
				SessionPwdParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 보조: 콘텐츠 선택 모달 옵션(tbm01 스코프 필터 재사용). */
	@GetMapping("/content-options")
	public ResponseEntity<?> getContentOptions(@ModelAttribute OptionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		ContentOptionResponse response = tbm02Service.selectContentOptions(
				OptionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 보조: 위험성평가 선택 모달 옵션. */
	@GetMapping("/risk-options")
	public ResponseEntity<?> getRiskOptions(@ModelAttribute OptionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		RiskOptionResponse response = tbm02Service.selectRiskOptions(
				OptionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 보조: 사업장 선택 드롭다운 옵션. */
	@GetMapping("/site-options")
	public ResponseEntity<?> getSiteOptions(@ModelAttribute OptionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SiteOptionResponse response = tbm02Service.selectSiteOptions(
				OptionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
