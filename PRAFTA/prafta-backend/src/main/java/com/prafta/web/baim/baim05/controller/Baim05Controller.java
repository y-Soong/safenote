package com.prafta.web.baim.baim05.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.baim.baim05.application.param.ClearDailyUserSlotsParam;
import com.prafta.web.baim.baim05.application.param.DailyUserLinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.DailyUserSlotListParam;
import com.prafta.web.baim.baim05.application.param.InsertDailyQrUserParam;
import com.prafta.web.baim.baim05.application.param.LinkPoliciesParam;
import com.prafta.web.baim.baim05.application.param.SetSlotFixedParam;
import com.prafta.web.baim.baim05.application.param.SetSlotNodeParam;
import com.prafta.web.baim.baim05.application.param.SetSlotTypeParam;
import com.prafta.web.baim.baim05.application.param.SlotHisParam;
import com.prafta.web.baim.baim05.dto.request.ClearDailyUserSlotsRequest;
import com.prafta.web.baim.baim05.dto.request.DailyUserLinkPoliciesRequest;
import com.prafta.web.baim.baim05.dto.request.DailyUserSlotListRequest;
import com.prafta.web.baim.baim05.dto.request.InsertDailyQrUserRequest;
import com.prafta.web.baim.baim05.dto.request.LinkPoliciesRequest;
import com.prafta.web.baim.baim05.dto.request.SetSlotFixedRequest;
import com.prafta.web.baim.baim05.dto.request.SetSlotNodeRequest;
import com.prafta.web.baim.baim05.dto.request.SetSlotTypeRequest;
import com.prafta.web.baim.baim05.dto.response.DailyUserLinkPoliciesResponse;
import com.prafta.web.baim.baim05.dto.response.DailyUserSlotListResponse;
import com.prafta.web.baim.baim05.dto.response.InsertDailyQrUserResponse;
import com.prafta.web.baim.baim05.dto.response.SlotHisListResponse;
import com.prafta.web.baim.baim05.service.Baim05Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/baim05")
@RequiredArgsConstructor
public class Baim05Controller { 	
	
	private final Baim05Service baim05Service;
	private final JwtUtil jwtUtil;
	
	@GetMapping("/daily-user-link-policies")
    public ResponseEntity<?> getDailyUserLinkPolicyList(@ModelAttribute DailyUserLinkPoliciesRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	DailyUserLinkPoliciesResponse response = baim05Service.selectDailyUserLinkPolicyList(DailyUserLinkPoliciesParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
		
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@GetMapping("/daily-user-slot-lists")
    public ResponseEntity<?> getDailyUserSlotList(@ModelAttribute DailyUserSlotListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	DailyUserSlotListResponse response = baim05Service.selectDailyUserSlotList(DailyUserSlotListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
	
	@PostMapping("/save-daily-user-link-policies")
	public ResponseEntity<?> saveDailyUserLinkPolicy(@RequestBody LinkPoliciesRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
		
		baim05Service.saveDailyUserLinkPolicy(LinkPoliciesParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/insert-daily-qr-user")
	public ResponseEntity<?> insertDailyQrUser(
			@Valid @RequestBody InsertDailyQrUserRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		InsertDailyQrUserResponse response = baim05Service.insertDailyQrUser(InsertDailyQrUserParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/clear-daily-user-slots")
	public ResponseEntity<?> clearDailyUserSlots(
			@RequestBody ClearDailyUserSlotsRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		baim05Service.clearDailyUserSlots(ClearDailyUserSlotsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/set-daily-user-slot-fixed")
	public ResponseEntity<?> setDailyUserSlotFixed(
			@RequestBody SetSlotFixedRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		baim05Service.setDailyUserSlotFixed(SetSlotFixedParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/set-daily-user-slot-type")
	public ResponseEntity<?> setDailyUserSlotType(
			@RequestBody SetSlotTypeRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		baim05Service.setDailyUserSlotType(SetSlotTypeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@PostMapping("/set-daily-user-slot-node")
	public ResponseEntity<?> setDailyUserSlotNode(
			@RequestBody SetSlotNodeRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		baim05Service.setDailyUserSlotNode(SetSlotNodeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@GetMapping("/daily-user-slot-his")
	public ResponseEntity<?> getDailyUserSlotHisList(
			@RequestParam(value = "siteCd", required = false) String siteCd,
			@RequestParam(value = "slotNo", required = false) String slotNo,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SlotHisListResponse response = baim05Service.selectDailyUserSlotHisList(
				SlotHisParam.from(siteCd, slotNo, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
