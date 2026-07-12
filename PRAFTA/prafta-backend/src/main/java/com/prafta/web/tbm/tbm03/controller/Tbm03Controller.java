package com.prafta.web.tbm.tbm03.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.tbm.tbm03.application.param.UserProgressDetailParam;
import com.prafta.web.tbm.tbm03.application.param.UserProgressListParam;
import com.prafta.web.tbm.tbm03.dto.request.UserProgressDetailRequest;
import com.prafta.web.tbm.tbm03.dto.request.UserProgressListRequest;
import com.prafta.web.tbm.tbm03.dto.response.UserProgressDetailResponse;
import com.prafta.web.tbm.tbm03.dto.response.UserProgressListResponse;
import com.prafta.web.tbm.tbm03.service.Tbm03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 진행관리(Tbm_03). PRAFTA-WEB_002-T7.
 *
 * <p>사용자별 TBM 누적 교육시간/수료·미이수/최근 이수일 집계 목록 + 세션 이수 이력 드릴다운. 순수 조회.
 */
@Slf4j
@RestController
@RequestMapping("/tbm03")
@RequiredArgsConstructor
public class Tbm03Controller {

	private final Tbm03Service tbm03Service;
	private final JwtUtil jwtUtil;

	/** 사용자별 진행 집계 목록(페이징, 사업장 스코프). */
	@GetMapping("/user-progress-list")
	public ResponseEntity<?> getUserProgressList(@ModelAttribute UserProgressListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		UserProgressListResponse response = tbm03Service.selectUserProgressList(
				UserProgressListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 사용자별 세션 이수 이력 드릴다운(userCd + userTypeCd 1명). */
	@GetMapping("/user-progress-detail")
	public ResponseEntity<?> getUserProgressDetail(@ModelAttribute UserProgressDetailRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		UserProgressDetailResponse response = tbm03Service.selectUserProgressDetail(
				UserProgressDetailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
