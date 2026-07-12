package com.prafta.web.dashboard.dashboard01.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.dashboard.dashboard01.application.param.DashAttdPlanRegRateParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashAttdStatusRateParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashSafetyAcctParam;
import com.prafta.web.dashboard.dashboard01.application.param.DashSafetyParam;
import com.prafta.web.dashboard.dashboard01.application.param.LeaveUsageParam;
import com.prafta.web.dashboard.dashboard01.application.param.OvertimeTrendParam;
import com.prafta.web.dashboard.dashboard01.dto.request.DashAttdPlanRegRateRequest;
import com.prafta.web.dashboard.dashboard01.dto.request.DashAttdStatusRateRequest;
import com.prafta.web.dashboard.dashboard01.dto.request.DashSafetyAcctRequest;
import com.prafta.web.dashboard.dashboard01.dto.request.DashSafetyRequest;
import com.prafta.web.dashboard.dashboard01.dto.request.LeaveUsageRequest;
import com.prafta.web.dashboard.dashboard01.dto.request.OvertimeTrendRequest;
import com.prafta.web.dashboard.dashboard01.service.Dashboard01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹 관리자 대시보드 컨트롤러 (PRAFTA-DASHBOARD-T1 골격 / T4 안전 탭 추가).
 * FE 호출 프리픽스: /webApi/dashboard01/* (ApiPrefixConfig가 /prafta/webApi 자동 부여)
 * 식별자(cmpnyCd/userCd)는 JWT 클레임에서만 도출하여 cross-site IDOR 을 차단한다.
 */
@Slf4j
@RestController
@RequestMapping("/dashboard01")
@RequiredArgsConstructor
public class Dashboard01Controller {

	private final Dashboard01Service dashboard01Service;
	private final JwtUtil jwtUtil;

	// S1 무사고 배너 + S5 사고 summary (안전 탭)
	@GetMapping("/safety-acct")
	public ResponseEntity<?> safetyAcct(
			@ModelAttribute DashSafetyAcctRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectSafetyAcct(
				DashSafetyAcctParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// S2 순회점검 — 당일 x/y + 조회월 미이행 수 (안전 탭, PRAFTA-DASHBOARD-T5)
	@GetMapping("/safety-patrol")
	public ResponseEntity<?> safetyPatrol(
			@ModelAttribute DashSafetyRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectSafetyPatrol(
				DashSafetyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// S3 위험성평가 — 검토요청/개선예정 카운트 + 조회월 아차사고 건수 (안전 탭, PRAFTA-DASHBOARD-T5)
	@GetMapping("/safety-risk")
	public ResponseEntity<?> safetyRisk(
			@ModelAttribute DashSafetyRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectSafetyRisk(
				DashSafetyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// S4 TBM — 조회월 포함 과거 12개월 완료 세션 건수 추이 (안전 탭, PRAFTA-DASHBOARD-T5)
	@GetMapping("/safety-tbm-trend")
	public ResponseEntity<?> safetyTbmTrend(
			@ModelAttribute DashSafetyRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectSafetyTbmTrend(
				DashSafetyParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// A1 근무계획 등록율 (근태 탭, PRAFTA-DASHBOARD-T2)
	@GetMapping("/attd-plan-reg-rate")
	public ResponseEntity<?> attdPlanRegRate(
			@ModelAttribute DashAttdPlanRegRateRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectAttdPlanRegRate(
				DashAttdPlanRegRateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// A2 정상/비정상 근무율 (근태 탭, PRAFTA-DASHBOARD-T2)
	@GetMapping("/attd-status-rate")
	public ResponseEntity<?> attdStatusRate(
			@ModelAttribute DashAttdStatusRateRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectAttdStatusRate(
				DashAttdStatusRateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// A3 초과근무 6개월 추이 (근태 탭, PRAFTA-DASHBOARD-T3)
	@GetMapping("/overtime-trend")
	public ResponseEntity<?> overtimeTrend(
			@ModelAttribute OvertimeTrendRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectOvertimeTrend(
				OvertimeTrendParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}

	// A4 법정연차 사용/사용예정/미사용 3분할 (근태 탭, PRAFTA-DASHBOARD-T3)
	@GetMapping("/leave-usage")
	public ResponseEntity<?> leaveUsage(
			@ModelAttribute LeaveUsageRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		return ResponseEntity.status(HttpStatus.OK).body(
			dashboard01Service.selectLeaveUsage(
				LeaveUsageParam.from(request, jwtUtil.getAllClaimsAsMap(authorization))));
	}
}
