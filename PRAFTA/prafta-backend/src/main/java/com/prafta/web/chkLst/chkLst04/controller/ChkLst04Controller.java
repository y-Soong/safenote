package com.prafta.web.chkLst.chkLst04.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.chkLst.chkLst04.application.param.ChkptTargetListParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectActionParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectListParam;
import com.prafta.web.chkLst.chkLst04.application.param.InspectItemListParam;
import com.prafta.web.chkLst.chkLst04.dto.request.ChkptTargetListRequest;
import com.prafta.web.chkLst.chkLst04.dto.request.DefectActionRequest;
import com.prafta.web.chkLst.chkLst04.dto.request.DefectListRequest;
import com.prafta.web.chkLst.chkLst04.dto.request.InspectItemListRequest;
import com.prafta.web.chkLst.chkLst04.dto.response.ChkptTargetListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.DefectListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.InspectItemListResponse;
import com.prafta.web.chkLst.chkLst04.service.ChkLst04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/chkLst04")
@RequiredArgsConstructor
public class ChkLst04Controller {

	private final ChkLst04Service chkLst04Service;
	private final JwtUtil jwtUtil;

	// 불량 목록 조회(불량만 INSPECT_ANSWER_TYPE='N' + 조치 LEFT JOIN)
	@GetMapping("/defect-lists")
	public ResponseEntity<?> getDefectList(@ModelAttribute DefectListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		DefectListResponse response = chkLst04Service.selectDefectList(
				DefectListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	// 점검대상 검색팝업(siteCd + chkLstType 필수)
	@GetMapping("/chkpt-target-lists")
	public ResponseEntity<?> getChkptTargetList(@ModelAttribute ChkptTargetListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		ChkptTargetListResponse response = chkLst04Service.selectChkptTargetList(
				ChkptTargetListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	// 점검문항 검색팝업(chkLstType 필수, chkLst04 전용 — 도메인 분리)
	@GetMapping("/inspect-item-lists")
	public ResponseEntity<?> getInspectItemList(@ModelAttribute InspectItemListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		InspectItemListResponse response = chkLst04Service.selectInspectItemList(
				InspectItemListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	// 조치 입력/수정 upsert
	@PostMapping("/save-defect-action")
	public ResponseEntity<?> saveDefectAction(@RequestBody DefectActionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		chkLst04Service.saveDefectAction(
				DefectActionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}
}
