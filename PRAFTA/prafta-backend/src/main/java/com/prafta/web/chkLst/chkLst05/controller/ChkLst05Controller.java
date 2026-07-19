package com.prafta.web.chkLst.chkLst05.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.chkLst.chkLst05.application.param.HistListParam;
import com.prafta.web.chkLst.chkLst05.dto.request.HistListRequest;
import com.prafta.web.chkLst.chkLst05.dto.response.AnswerHistListResponse;
import com.prafta.web.chkLst.chkLst05.dto.response.DefectHistListResponse;
import com.prafta.web.chkLst.chkLst05.service.ChkLst05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 순회점검 결과 덮어쓰기 이력 조회(PRAFTA-SUBCON-T6-AUDIT-03) — 읽기전용 감사 화면 ChkLst_05 백엔드.
 */
@Slf4j
@RestController
@RequestMapping("/chkLst05")
@RequiredArgsConstructor
public class ChkLst05Controller {

	private final ChkLst05Service chkLst05Service;
	private final JwtUtil jwtUtil;

	// 점검 응답 덮어쓰기 이력(좌표별 타임라인) — 사업장/기간 필수.
	@GetMapping("/answer-hists")
	public ResponseEntity<?> getAnswerHists(@ModelAttribute HistListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		AnswerHistListResponse response = chkLst05Service.selectAnswerHistList(
				HistListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	// 불량조치 덮어쓰기 이력(좌표별 타임라인) — 사업장/기간 필수.
	@GetMapping("/defect-hists")
	public ResponseEntity<?> getDefectHists(@ModelAttribute HistListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		DefectHistListResponse response = chkLst05Service.selectDefectHistList(
				HistListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
