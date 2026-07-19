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
import com.prafta.web.tbm.tbm02.application.param.EjectAttendanceParam;
import com.prafta.web.tbm.tbm02.application.param.EntryCandidateParam;
import com.prafta.web.tbm.tbm02.application.param.ManagerEnterParam;
import com.prafta.web.tbm.tbm02.application.param.OptionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionCancelParam;
import com.prafta.web.tbm.tbm02.application.param.SessionDetailParam;
import com.prafta.web.tbm.tbm02.application.param.SessionListParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPrepareParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPwdParam;
import com.prafta.web.tbm.tbm02.application.param.SessionSaveParam;
import com.prafta.web.tbm.tbm02.application.param.SessionShareParam;
import com.prafta.web.tbm.tbm02.application.param.SharedSessionListParam;
import com.prafta.web.tbm.tbm02.dto.request.SharedSessionListRequest;
import com.prafta.web.tbm.tbm02.dto.response.SharedSessionListResponse;
import com.prafta.web.tbm.tbm02.application.param.SessionTransitionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;
import com.prafta.web.tbm.tbm02.dto.request.EjectAttendanceRequest;
import com.prafta.web.tbm.tbm02.dto.request.EntryCandidateRequest;
import com.prafta.web.tbm.tbm02.dto.request.ManagerEnterRequest;
import com.prafta.web.tbm.tbm02.dto.request.OptionRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionCancelRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionDetailRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionListRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionPrepareRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionPwdRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionSaveRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionShareRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionTransitionRequest;
import com.prafta.web.tbm.tbm02.dto.request.SessionUpdateRequest;
import com.prafta.web.tbm.tbm02.dto.response.ContentOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.EntryCandidateResponse;
import com.prafta.web.tbm.tbm02.dto.response.ManagerEnterResponse;
import com.prafta.web.tbm.tbm02.dto.response.RiskOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionAttendanceListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionCompleteResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionDetailResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionExitPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPrepareResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionSaveResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionShareCandidateResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionShareListResponse;
import com.prafta.web.tbm.tbm02.dto.response.ShareAllowedCmpnyResponse;
import com.prafta.web.tbm.tbm02.dto.response.SiteOptionResponse;
import com.prafta.web.tbm.tbm02.service.Tbm02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 세션 관리(W-04~06 + 상태머신 재설계). prafta-033-B / prafta-051.
 *
 * <p>상태 흐름(prafta-051): 개설(DRAFT) → 교육준비(OPENED) → 교육시작(IN_PROGRESS)
 * → 교육종료(COMPLETED), 취소(CANCELLED)는 DRAFT/OPENED에서만.
 * 동기화/QR출결은 앱(tbm01) 소관이므로 본 컨트롤러에 없다.
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

	/** 입실 비밀번호 재발급(OPENED만, 입실비번 전용). */
	@PostMapping(value = "/regenerate-passwords", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> regeneratePasswords(@RequestBody SessionPwdRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionPwdResponse response = tbm02Service.regeneratePasswords(
				SessionPwdParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 종료 비밀번호 재발급(COMPLETED만, 종료비번 전용, prafta-051-02). */
	@PostMapping(value = "/regenerate-exit-password", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> regenerateExitPassword(@RequestBody SessionTransitionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionExitPwdResponse response = tbm02Service.regenerateExitPassword(
				SessionTransitionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 교육준비(OPENED) 전이 + 입실비번 발급 + GPS 중심좌표(prafta-051-03). */
	@PostMapping(value = "/prepare-session", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> prepareSession(@RequestBody SessionPrepareRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionPrepareResponse response = tbm02Service.prepareSession(
				SessionPrepareParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 교육시작(IN_PROGRESS) 수동 전이(OPENED만, prafta-051-04). */
	@PostMapping(value = "/start-session", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> startSession(@RequestBody SessionTransitionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.startSession(
				SessionTransitionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	/** 교육준비 연장(OPENED + 15분 미도래만, PREP_START_AT 리셋, prafta-051-04). */
	@PostMapping(value = "/extend-prep", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> extendPrep(@RequestBody SessionTransitionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.extendPrep(
				SessionTransitionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	/** 교육종료(COMPLETED) 전이 + 종료비번 발급(IN_PROGRESS만, prafta-051-05). */
	@PostMapping(value = "/complete-session", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> completeSession(@RequestBody SessionTransitionRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionCompleteResponse response = tbm02Service.completeSession(
				SessionTransitionParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 입실 후보 검색(정규직/일용직, prafta-051-11). */
	@GetMapping("/entry-candidates")
	public ResponseEntity<?> getEntryCandidates(@ModelAttribute EntryCandidateRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		EntryCandidateResponse response = tbm02Service.selectEntryCandidates(
				EntryCandidateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 관리자 직접 입실(MANAGER_DIRECT, OPENED만, prafta-051-11). */
	@PostMapping(value = "/manager-enter", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> managerEnter(@RequestBody ManagerEnterRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		ManagerEnterResponse response = tbm02Service.managerEnter(
				ManagerEnterParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 교육준비 단계 입실자 명단(거리/입실유형, prafta-051-12). */
	@GetMapping("/session-attendances")
	public ResponseEntity<?> getSessionAttendances(@ModelAttribute SessionDetailRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionAttendanceListResponse response = tbm02Service.selectSessionAttendances(
				SessionDetailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** 입실자 내보내기(soft delete, OPENED만, 사유 필수, prafta-051-12). */
	@PostMapping(value = "/eject-attendance", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> ejectAttendance(@RequestBody EjectAttendanceRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.ejectAttendance(
				EjectAttendanceParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	// ============================ PRAFTA-SUBCON-T5 연동 회사 지정 ============================

	/**
	 * T5 D2: 연동받은 교육 목록(비개설사 전용).
	 *
	 * <p>내 회사가 유효하게 지정받은 타사 세션의 헤더 최소 필드만 반환한다(재지정 진입점).
	 * 세션 상세/콘솔/참석자 API 는 개설사 전용 게이트를 그대로 유지하므로 이 목록에서 진입할 수 없다.
	 */
	@GetMapping("/shared-sessions")
	public ResponseEntity<?> getSharedSessions(@ModelAttribute SharedSessionListRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SharedSessionListResponse response = tbm02Service.selectSharedSessionList(
				SharedSessionListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** T5: 연동 회사 지정 후보(관계 ACCEPTED − 개설사 − 이미 체인에 있는 회사). */
	@GetMapping("/session-share-candidates")
	public ResponseEntity<?> getSessionShareCandidates(@ModelAttribute SessionShareRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionShareCandidateResponse response = tbm02Service.selectShareCandidates(
				SessionShareParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** T5: 연동 회사 지정 현황(내가 직접 지정한 회사 + 하위 재지정 개사 수). */
	@GetMapping("/session-shares")
	public ResponseEntity<?> getSessionShares(@ModelAttribute SessionShareRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		SessionShareListResponse response = tbm02Service.selectSessionShares(
				SessionShareParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** T5: 입실 대상 회사 목록(개설사 + 지정 체인, 서버 relabel). 대리입실 팝업의 대상 회사 셀렉트. */
	@GetMapping("/session-share-allowed-cmpnys")
	public ResponseEntity<?> getShareAllowedCmpnys(@ModelAttribute SessionShareRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		ShareAllowedCmpnyResponse response = tbm02Service.selectAllowedCmpnys(
				SessionShareParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	/** T5: 연동 회사 지정(DRAFT/OPENED 만). */
	@PostMapping(value = "/session-share-designate", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> designateSessionShare(@RequestBody SessionShareRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.designateShare(
				SessionShareParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
	}

	/** T5: 연동 회사 지정 해제(자기 지정분만) + 하위 재지정 캐스케이드. */
	@PostMapping(value = "/session-share-release", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> releaseSessionShare(@RequestBody SessionShareRequest request,
			@RequestHeader(value = "Authorization", required = false) String authorization) {

		tbm02Service.releaseShare(
				SessionShareParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

		return ResponseEntity.ok().build();
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
