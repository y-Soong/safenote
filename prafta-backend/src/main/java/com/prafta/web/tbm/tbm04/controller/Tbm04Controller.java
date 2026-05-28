package com.prafta.web.tbm.tbm04.controller;

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
import com.prafta.web.tbm.tbm04.application.param.AttendanceEventParam;
import com.prafta.web.tbm.tbm04.application.param.CompletionUpdateParam;
import com.prafta.web.tbm.tbm04.application.param.HistorySessionListParam;
import com.prafta.web.tbm.tbm04.application.param.SessionAttendanceParam;
import com.prafta.web.tbm.tbm04.application.param.UserAttendanceParam;
import com.prafta.web.tbm.tbm04.dto.request.AttendanceEventRequest;
import com.prafta.web.tbm.tbm04.dto.request.CompletionUpdateRequest;
import com.prafta.web.tbm.tbm04.dto.request.HistorySessionListRequest;
import com.prafta.web.tbm.tbm04.dto.request.SessionAttendanceRequest;
import com.prafta.web.tbm.tbm04.dto.request.UserAttendanceRequest;
import com.prafta.web.tbm.tbm04.dto.response.AttendanceEventResponse;
import com.prafta.web.tbm.tbm04.dto.response.HistorySessionListResponse;
import com.prafta.web.tbm.tbm04.dto.response.SessionAttendanceResponse;
import com.prafta.web.tbm.tbm04.dto.response.UserAttendanceResponse;
import com.prafta.web.tbm.tbm04.service.Tbm04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 이력 관리(W-12~15). prafta-033-D.
 *
 * <p>출결/이벤트는 읽기 전용 + W-14 사후 보정 UPDATE 만. 입실/종료/QR/SSE 쓰기는 C·앱 소관(미구현).
 * 출결 데이터가 비어 있어도 모든 조회는 빈 상태로 정상 동작한다.
 */
@Slf4j
@RestController
@RequestMapping("/tbm04")
@RequiredArgsConstructor
public class Tbm04Controller {

	private final Tbm04Service tbm04Service;
	private final JwtUtil jwtUtil;

	/** W-12 이력 목록(COMPLETED/CANCELLED 위주 + 기간 통계). */
	@GetMapping("/history-sessions")
	public ResponseEntity<?> getHistorySessions(@ModelAttribute HistorySessionListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		HistorySessionListResponse response = tbm04Service.selectHistorySessionList(
				HistorySessionListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-13 세션 출결 명단(유형별 분기 조인, 이상신호 요약). */
	@GetMapping("/session-attendances")
	public ResponseEntity<?> getSessionAttendances(@ModelAttribute SessionAttendanceRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionAttendanceResponse response = tbm04Service.selectSessionAttendances(
				SessionAttendanceParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-13 출결 단건 이벤트 타임라인(시간순). */
	@GetMapping("/attendance-events")
	public ResponseEntity<?> getAttendanceEvents(@ModelAttribute AttendanceEventRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		AttendanceEventResponse response = tbm04Service.selectAttendanceEvents(
				AttendanceEventParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-14 미이수 처리(이수/미이수 사후 변경, 사유 10자 이상, 권한 개설자/safe/master). */
	@PostMapping(value = "/update-completion", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateCompletion(@RequestBody CompletionUpdateRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm04Service.updateCompletion(
				CompletionUpdateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	/** W-15 정규직 사용자별 이수 이력. */
	@GetMapping("/user-attendances")
	public ResponseEntity<?> getUserAttendances(@ModelAttribute UserAttendanceRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		UserAttendanceResponse response = tbm04Service.selectUserAttendances(
				UserAttendanceParam.from(request, jwtUtil.getAllClaimsAsMap(authorization), "REGULAR"));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** W-15 일용직 사용자별 이수 이력. */
	@GetMapping("/daily-user-attendances")
	public ResponseEntity<?> getDailyUserAttendances(@ModelAttribute UserAttendanceRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		UserAttendanceResponse response = tbm04Service.selectUserAttendances(
				UserAttendanceParam.from(request, jwtUtil.getAllClaimsAsMap(authorization), "DAILY"));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
