package com.prafta.web.tbm.tbm02.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.push.TbmEventNotiService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.NumericPwdGenerator;
import com.prafta.web.tbm.tbm02.application.command.SessionCancelCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionContentCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionPrepareCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionRiskCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionSinglePwdCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionStateCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionStateTransitionCommand;
import com.prafta.web.tbm.tbm02.application.command.EjectAttendanceCommand;
import com.prafta.web.tbm.tbm02.application.command.ManagerEnterCommand;
import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;
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
import com.prafta.web.tbm.tbm02.application.param.SessionTransitionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;
import com.prafta.web.tbm.tbm02.application.query.OptionQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionDetailQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionListQuery;
import com.prafta.web.tbm.tbm02.application.query.EntryCandidateQuery;
import com.prafta.web.tbm.tbm02.application.query.EntryTargetQuery;
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
import com.prafta.web.tbm.tbm02.dto.response.SiteOptionResponse;
import com.prafta.web.tbm.tbm02.mapper.Tbm02Mapper;
import com.prafta.web.tbm.tbm02.result.AttendanceSlotResult;
import com.prafta.web.tbm.tbm02.result.EntryCandidateResult;
import com.prafta.web.tbm.tbm02.result.RiskOptionResult;
import com.prafta.web.tbm.tbm02.result.SessionAttendanceResult;
import com.prafta.web.tbm.tbm02.result.SessionContentResult;
import com.prafta.web.tbm.tbm02.result.SessionGuardResult;
import com.prafta.web.tbm.tbm02.result.SessionListResult;
import com.prafta.web.tbm.tbm02.result.SessionResult;
import com.prafta.web.tbm.tbm02.result.SessionRiskResult;
import com.prafta.web.tbm.tbm02.service.Tbm02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Tbm02ServiceImpl implements Tbm02Service {

	private final Tbm02Mapper tbm02Mapper;
	/** PRAFTA-APP-021-3b(W3): 웹 admin 라이브 경로 TBM 시작/종료 통보 PUSH 생산자(입실 참석자 대상, afterCommit 격리). */
	private final TbmEventNotiService tbmEventNotiService;

	/** 비밀번호 자리수(랜덤 6자리). */
	private static final int PWD_LENGTH = 6;

	/** GPS 검증 반경 범위(05_02 §2.2). */
	private static final int GPS_RADIUS_MIN = 50;
	private static final int GPS_RADIUS_MAX = 1000;

	/** 교육 내용 최소 텍스트 길이(빈 HTML 거부). */
	private static final int CONTENT_TEXT_MIN = 10;

	/** 교육 인정시간(분) 범위(요청서 17.3.1 / 공유계약: 1~60 정수). */
	private static final int EDU_MINUTES_MIN = 1;
	private static final int EDU_MINUTES_MAX = 60;

	/** 위험성평가 미연동 경고 메시지(05_02 §1.4 / §3.4). */
	private static final String RISK_WARNING_MESSAGE =
		"위험성평가 연동되지 않은 TBM입니다. 사고 발생 시 설득력이 떨어질 수 있습니다.";

	// ============================ W-04 목록 ============================

	@Override
	public SessionListResponse selectSessionList(SessionListParam param) {
		// 999999(권한 미부여)는 화면 진입 차단
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 세션 목록 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}

		SessionListQuery query = SessionListQuery.from(param);

		List<SessionListResult> list = tbm02Mapper.selectSessionList(query);
		int totalCount = tbm02Mapper.selectSessionListCount(query);

		log.info("TBM 세션 목록 조회 완료 - cmpnyCd={}, count={}, totalCount={}",
				param.gvCmpnyCd(), list != null ? list.size() : 0, totalCount);

		return SessionListResponse.builder()
				.sessionList(list != null ? list : Collections.emptyList())
				.totalCount(totalCount)
				.page(param.page())
				.pageSize(param.pageSize())
				.build();
	}

	// ============================ W-06 상세 ============================

	@Override
	public SessionDetailResponse selectSessionDetail(SessionDetailParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 세션 상세 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}

		SessionDetailQuery query = SessionDetailQuery.from(param);

		SessionResult session = tbm02Mapper.selectSessionDetail(query);
		if (session == null) {
			throw new ApiException(TbmErrorCode.TBM_404_010);
		}

		// 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 세션만 열람 가능
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), session.siteCd());

		List<SessionContentResult> contents = tbm02Mapper.selectSessionContents(query);
		List<SessionRiskResult> risks = tbm02Mapper.selectSessionRisks(query);

		// 비밀번호 노출 게이트(T6-03 필드별 분리):
		//  - 입실비번: 교육준비(OPENED)/교육시작(IN_PROGRESS) + 관리자
		//  - 종료비번: 교육종료(COMPLETED) + 관리자 (재발급 후 fnSearch 덮어쓰기로 사라지던 결함 해소)
		boolean entryPwdVisible = isEntryPwdVisible(session.statusCd(), param.gvAuthCd());
		boolean exitPwdVisible = isExitPwdVisible(session.statusCd(), param.gvAuthCd());
		boolean pwdVisible = entryPwdVisible || exitPwdVisible;

		// [Medium 보강/T6] 위험성평가 상세 민감필드 노출 게이트(비번 게이트와 동일 출처: 서버 권위 authCd).
		// 비관리자(일반 근로자)가 sessionCd 를 열거해 직접 호출해도 평가자 실명/파일경로/점수·설명 상세는
		// redaction 한다. 콘솔은 관리자 전용 화면이라 관리자(master/safe/hr)는 기존과 동일하게 전부 노출.
		boolean riskDetailVisible = isManagerRole(param.gvAuthCd());

		SessionDetailResponse.SessionDetailItem item = SessionDetailResponse.SessionDetailItem.builder()
				.sessionCd(session.sessionCd())
				.siteCd(session.siteCd())
				.siteNm(session.siteNm())
				.eduTypeCd(session.eduTypeCd())
				.title(session.title())
				.contentBody(session.contentBody())
				.contentFormatCd(session.contentFormatCd())
				.statusCd(session.statusCd())
				.statusNm(session.statusNm())
				.entryPwd(entryPwdVisible ? session.entryPwd() : null)
				.exitPwd(exitPwdVisible ? session.exitPwd() : null)
				.pwdVisible(pwdVisible)
				.managerUserCd(session.managerUserCd())
				.managerUserNm(session.managerUserNm())
				.managerGpsLat(session.managerGpsLat())
				.managerGpsLon(session.managerGpsLon())
				.gpsVerifyTypeCd(session.gpsVerifyTypeCd())
				.gpsVerifyRadiusM(session.gpsVerifyRadiusM())
				.eduMinutes(session.eduMinutes())
				.gpsManualConfirmYn(session.gpsManualConfirmYn())
				.openedAt(session.openedAt())
				.prepStartAt(session.prepStartAt())
				.startedAt(session.startedAt())
				.endedAt(session.endedAt())
				.cancelledAt(session.cancelledAt())
				.cancelReason(session.cancelReason())
				.insertNm(session.insertNm())
				.insertDate(session.insertDate())
				.build();

		List<SessionDetailResponse.SessionRiskItem> riskItems = new ArrayList<>();
		if (risks != null) {
			for (SessionRiskResult r : risks) {
				riskItems.add(buildSessionRiskItem(r, riskDetailVisible));
			}
		}

		log.info("TBM 세션 상세 조회 완료 - sessionCd={}, status={}, pwdVisible={}, riskDetailVisible={}",
				session.sessionCd(), session.statusCd(), pwdVisible, riskDetailVisible);

		return SessionDetailResponse.builder()
				.session(item)
				.contents(contents != null ? contents : Collections.emptyList())
				.risks(riskItems)
				.build();
	}

	// ============================ W-05 개설/임시저장 ============================

	/**
	 * 세션 개설(prafta-051 C2). 개설 결과는 <b>항상 DRAFT</b>로 고정한다.
	 *
	 * <p>비밀번호 발급/GPS 좌표 검증/상태 row 초기화는 모두 교육준비(prepare) 전이로 이동했으므로
	 * 여기서는 수행하지 않는다(미완성 허용). 제목 필수 + 사업장 스코프 검증만 강제하며, 교육 내용/GPS는
	 * DRAFT 단계에서 강제하지 않는다. 응답의 entryPwd/exitPwd 는 항상 null.
	 */
	@Override
	@Transactional
	public SessionSaveResponse saveSession(SessionSaveParam param) {
		// 권한 게이트: 개설=safe + 회사 전체 권한(master/safe). 999999 차단.
		verifyManageAuth(param.gvAuthCd());

		// 사업장 필수 + 스코프 격리(회사 전체 권한이 아니면 자기 사업장만)
		if (!StringUtils.hasText(param.siteCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

		// 제목 검증(개설 시점 유일 필수 항목)
		String title = param.title() != null ? param.title().trim() : "";
		if (!StringUtils.hasText(title) || title.length() > 200) {
			throw new ApiException(TbmErrorCode.TBM_400_011);
		}

		// 교육시간 검증(개설은 NULL 허용, 있으면 1~60)
		validateEduMinutes(param.eduMinutes(), false);

		String sessionCd = tbm02Mapper.selectSessionCd(param.gvCmpnyCd());
		if (!StringUtils.hasText(sessionCd)) {
			log.error("TBM 세션코드 채번 실패 - cmpnyCd={}", param.gvCmpnyCd());
			throw new ApiException(CommonErrorCode.COMMON_500_001);
		}

		// 1. 세션 헤더 INSERT(DRAFT 고정, 비번/OPENED_AT 미설정)
		tbm02Mapper.insertSession(SessionCommand.forSave(param, sessionCd));

		// 2. 콘텐츠 매핑 INSERT
		insertContents(param.contents(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());

		// 3. 위험성평가 매핑 INSERT(옵션)
		insertRisks(param.risks(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());

		// 상태 row 초기화(upsertSessionState)는 교육준비(prepare) 전이로 이동(개설 시 미수행).

		boolean riskEmpty = param.risks() == null || param.risks().isEmpty();

		log.info("TBM 세션 개설 완료(DRAFT 고정) - sessionCd={}, riskEmpty={}", sessionCd, riskEmpty);

		return SessionSaveResponse.builder()
				.sessionCd(sessionCd)
				.statusCd("DRAFT")
				.entryPwd(null)
				.exitPwd(null)
				.warningMessage(riskEmpty ? RISK_WARNING_MESSAGE : null)
				.build();
	}

	// ============================ 교육준비(OPENED) 전이 (prafta-051-03) ============================

	/**
	 * 교육준비(OPENED) 전이. DRAFT 세션을 교육준비 상태로 전이하며 입실비번 발급 + 관리자 GPS
	 * 중심좌표 + PREP_START_AT 기준시각을 확정한다(15분 자동 교육시작 타이머 시작).
	 *
	 * <p>전이 가능 시점: DRAFT 만(C3). OPENED 이상은 거부(TBM_409_013). 교육 내용 텍스트 &gt;= 10자,
	 * GPS 검증(AUTO면 좌표 필수)을 강제한다. 종료비번(EXIT_PWD)은 발급하지 않는다(교육종료 소관).
	 */
	@Override
	@Transactional
	public SessionPrepareResponse prepareSession(SessionPrepareParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		// 전이 가능 시점: DRAFT 만(C3)
		if (!"DRAFT".equals(guard.statusCd())) {
			log.warn("TBM 교육준비 전이 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_013);
		}

		// §X-2(Q5-A): 교육준비는 경량 전이(GPS 좌표 확정 + 상태전이 + 입실비번 발급 + 교육시간 필수검증)만 수행한다.
		// 제목/교육내용 검증은 FE 가 보내지 않는 param.title()/contentBody() 가 아니라 DB 의 DRAFT 값 기준으로 한다.
		// 콘텐츠/위험성 매핑은 개설/수정 단계에서 이미 확정되므로 prepare 에서 delete-reinsert 하지 않는다(데이터 손실 방지).

		// 제목 검증(DB 의 DRAFT 값 기준)
		String title = guard.title() != null ? guard.title().trim() : "";
		if (!StringUtils.hasText(title) || title.length() > 200) {
			log.warn("TBM 교육준비 제목 부적합(DB 기준) - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_400_011);
		}

		// 교육 내용 검증(DB 의 DRAFT 값 기준)
		validateContentBody(guard.contentBody());

		// 교육시간 필수 검증(DB 의 DRAFT 값 기준, 미입력이면 TBM_400_015)
		validateEduMinutes(guard.eduMinutes(), true);

		// GPS 검증(개설 수준): 좌표는 교육준비 시점에 param 으로 받아 확정(현행 유지)
		validateGps(param.gpsVerifyTypeCd(), param.managerGpsLat(), param.managerGpsLon(),
				param.gpsVerifyRadiusM(), param.gpsManualConfirmYn());

		// 입실비번 발급(6자리). 종료비번은 발급하지 않음(null 유지).
		String entryPwd = NumericPwdGenerator.generate(PWD_LENGTH);

		// 교육준비 전이 UPDATE(WHERE STATUS_CD='DRAFT' 경합 가드)
		int affected = tbm02Mapper.prepareSession(SessionPrepareCommand.of(param, entryPwd));
		if (affected == 0) {
			// 동시 전이/상태 변경으로 DRAFT 가 아님
			log.warn("TBM 교육준비 전이 경합/상태부적합 - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_409_013);
		}

		// 상태 row 초기 UPSERT(C 단계 동기화 상태 초기값) — 개설에서 이 시점으로 이동
		tbm02Mapper.upsertSessionState(
				SessionStateCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));

		boolean riskEmpty = param.risks() == null || param.risks().isEmpty();

		log.info("TBM 교육준비 전이 완료(OPENED) - sessionCd={}, riskEmpty={}", param.sessionCd(), riskEmpty);

		return SessionPrepareResponse.builder()
				.sessionCd(param.sessionCd())
				.statusCd("OPENED")
				.entryPwd(entryPwd)
				.warningMessage(riskEmpty ? RISK_WARNING_MESSAGE : null)
				.build();
	}

	// ============================ 교육시작(IN_PROGRESS)/연장 (prafta-051-04) ============================

	/**
	 * 교육시작(IN_PROGRESS) 수동 전이. OPENED 세션만 전이 가능(C). STARTED_AT=NOW().
	 *
	 * <p>입실 마감은 앱 enter 가 OPENED 만 허용하므로 자동 충족된다(prafta-051-07 정합).
	 * WHERE STATUS_CD='OPENED' 경합 가드로 자동시작 배치와 원자적.
	 */
	@Override
	@Transactional
	public void startSession(SessionTransitionParam param) {
		SessionGuardResult guard = loadTransitionGuard(param);

		if (!"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 교육시작 전이 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_014);
		}

		int affected = tbm02Mapper.startSession(
				SessionStateTransitionCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));
		if (affected == 0) {
			// 자동시작 배치/동시 전이로 OPENED 가 아님
			log.warn("TBM 교육시작 전이 경합/상태부적합 - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_409_014);
		}

		log.info("TBM 교육시작 전이 완료(IN_PROGRESS) - sessionCd={}", param.sessionCd());

		// PRAFTA-APP-021-3b(W3): 웹 라이브 경로도 교육 시작 시 입실 참석자에게 PUSH 적재
		// (앱 admin AppAdminTbmServiceImpl.startSession 과 동일 시그니처/패턴, afterCommit 격리·전이 영향 없음).
		try {
			tbmEventNotiService.notifyTbmStarted(
					param.gvCmpnyCd(), guard.siteCd(), param.sessionCd(), param.gvUserCd());
		} catch (Exception e) {
			log.error("TBM 교육 시작 통보 PUSH 적재 hook 실패(전이 영향 없음). sessionCd={}", param.sessionCd(), e);
		}
	}

	/**
	 * 교육준비 연장. OPENED 이면서 PREP_START_AT+15분 미도래일 때만 PREP_START_AT 를 NOW() 로
	 * 리셋해 15분 타이머를 재시작한다. 경과/상태부적합이면 거부(TBM_409_015).
	 *
	 * <p>경합 가드: UPDATE WHERE STATUS_CD='OPENED' AND PREP_START_AT &gt; NOW()-15분.
	 * 영향행 0이면 자동전이 임박/완료 또는 상태부적합으로 판단해 거부한다.
	 */
	@Override
	@Transactional
	public void extendPrep(SessionTransitionParam param) {
		SessionGuardResult guard = loadTransitionGuard(param);

		// 상태 1차 확인(경합은 UPDATE WHERE 가 최종 판정)
		if (!"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 교육준비 연장 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_015);
		}

		int affected = tbm02Mapper.extendPrep(
				SessionStateTransitionCommand.of(param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd()));
		if (affected == 0) {
			// 15분 경과(자동전이 임박/완료) 또는 OPENED 아님
			log.warn("TBM 교육준비 연장 경합/시간경과 - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_409_015);
		}

		log.info("TBM 교육준비 연장 완료(PREP_START_AT 리셋) - sessionCd={}", param.sessionCd());
	}

	// ============================ 교육종료(COMPLETED) (prafta-051-05) ============================

	/**
	 * 교육종료(COMPLETED) 전이. IN_PROGRESS 세션만 전이 가능(C4). ENDED_AT=NOW() +
	 * 종료비번 신규 발급(입실비번 미변경). WHERE STATUS_CD='IN_PROGRESS' 경합 가드.
	 */
	@Override
	@Transactional
	public SessionCompleteResponse completeSession(SessionTransitionParam param) {
		SessionGuardResult guard = loadTransitionGuard(param);

		if (!"IN_PROGRESS".equals(guard.statusCd())) {
			log.warn("TBM 교육종료 전이 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_016);
		}

		// 종료비번 발급(6자리). 입실비번은 미변경.
		String exitPwd = NumericPwdGenerator.generate(PWD_LENGTH);

		int affected = tbm02Mapper.completeSession(SessionStateTransitionCommand.ofComplete(
				param.sessionCd(), exitPwd, param.gvCmpnyCd(), param.gvUserCd()));
		if (affected == 0) {
			log.warn("TBM 교육종료 전이 경합/상태부적합 - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_409_016);
		}

		log.info("TBM 교육종료 전이 완료(COMPLETED) - sessionCd={}", param.sessionCd());

		// PRAFTA-APP-021-3b(W3): 웹 라이브 경로도 교육 종료 시 입실 참석자에게 PUSH 적재
		// (앱 admin AppAdminTbmServiceImpl.endSession 과 동일 시그니처/패턴, afterCommit 격리·전이 영향 없음).
		try {
			tbmEventNotiService.notifyTbmCompleted(
					param.gvCmpnyCd(), guard.siteCd(), param.sessionCd(), param.gvUserCd());
		} catch (Exception e) {
			log.error("TBM 교육 종료 통보 PUSH 적재 hook 실패(전이 영향 없음). sessionCd={}", param.sessionCd(), e);
		}

		return SessionCompleteResponse.builder()
				.sessionCd(param.sessionCd())
				.statusCd("COMPLETED")
				.exitPwd(exitPwd)
				.build();
	}

	/**
	 * 종료 비밀번호 재발급(COMPLETED 만). 입실비번은 미변경.
	 *
	 * <p>기종료자 무영향: 종료비번은 exit 시점에만 검증되고 결과는 TB_TBM_ATTENDANCE 에 확정되므로,
	 * 본 재발급은 SESSION 만 UPDATE 하고 ATTENDANCE 는 미변경한다.
	 */
	@Override
	@Transactional
	public SessionExitPwdResponse regenerateExitPassword(SessionTransitionParam param) {
		SessionGuardResult guard = loadTransitionGuard(param);

		if (!"COMPLETED".equals(guard.statusCd())) {
			log.warn("TBM 종료비번 재발급 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_017);
		}

		String exitPwd = NumericPwdGenerator.generate(PWD_LENGTH);
		int affected = tbm02Mapper.updateExitPwd(SessionSinglePwdCommand.of(
				param.sessionCd(), exitPwd, param.gvCmpnyCd(), param.gvUserCd()));
		if (affected == 0) {
			log.warn("TBM 종료비번 재발급 경합/상태부적합 - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_409_017);
		}

		log.info("TBM 종료비번 재발급 완료 - sessionCd={}", param.sessionCd());

		return SessionExitPwdResponse.builder()
				.sessionCd(param.sessionCd())
				.exitPwd(exitPwd)
				.build();
	}

	// ============================ W-06 수정 ============================

	@Override
	@Transactional
	public void updateSession(SessionUpdateParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		// 수정 가능 시점: DRAFT / OPENED 만
		if (!"DRAFT".equals(guard.statusCd()) && !"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 세션 수정 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_010);
		}

		// 제목 검증
		String title = param.title() != null ? param.title().trim() : "";
		if (!StringUtils.hasText(title) || title.length() > 200) {
			throw new ApiException(TbmErrorCode.TBM_400_011);
		}

		// 교육시간 검증(수정은 NULL 허용, 있으면 1~60)
		validateEduMinutes(param.eduMinutes(), false);

		// OPENED 상태 수정 시 교육 내용/ GPS 는 개설 수준으로 재검증
		if ("OPENED".equals(guard.statusCd())) {
			validateContentBody(param.contentBody());
			validateGps(param.gpsVerifyTypeCd(), param.managerGpsLat(), param.managerGpsLon(),
					param.gpsVerifyRadiusM(), param.gpsManualConfirmYn());
		}

		// 1. 세션 헤더 UPDATE(비번/상태 제외)
		tbm02Mapper.updateSession(SessionCommand.forUpdate(param));

		// 2. 매핑 재구성(기존 delete 후 재insert)
		tbm02Mapper.deleteSessionContents(param.gvCmpnyCd(), param.sessionCd());
		insertContents(param.contents(), param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd());

		tbm02Mapper.deleteSessionRisks(param.gvCmpnyCd(), param.sessionCd());
		insertRisks(param.risks(), param.sessionCd(), param.gvCmpnyCd(), param.gvUserCd());

		log.info("TBM 세션 수정 완료 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
	}

	// ============================ W-06 취소 ============================

	@Override
	@Transactional
	public void cancelSession(SessionCancelParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		if (!StringUtils.hasText(param.cancelReason())) {
			throw new ApiException(TbmErrorCode.TBM_400_014);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		// 취소 가능 시점: DRAFT / OPENED 만(IN_PROGRESS 이후는 강제종료=C 소관)
		if (!"DRAFT".equals(guard.statusCd()) && !"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 세션 취소 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_011);
		}

		tbm02Mapper.cancelSession(SessionCancelCommand.from(param));

		log.info("TBM 세션 취소 완료 - sessionCd={}, prevStatus={}", param.sessionCd(), guard.statusCd());
	}

	// ============================ W-06 비밀번호 재발급 ============================

	/**
	 * 입실 비밀번호 재발급(OPENED 만, prafta-051-02). <b>입실비번만</b> 재발급하며 종료비번은 미변경.
	 *
	 * <p>기입실자 무영향: 입실비번은 enter 시점에만 검증되고 결과는 TB_TBM_ATTENDANCE 에 확정되므로,
	 * 본 재발급은 SESSION 만 UPDATE 하고 ATTENDANCE 는 미변경한다. 응답의 exitPwd 는 null.
	 */
	@Override
	@Transactional
	public SessionPwdResponse regeneratePasswords(SessionPwdParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		// 재발급 가능 시점: OPENED 만
		if (!"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 세션 입실비번 재발급 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_012);
		}

		String entryPwd = NumericPwdGenerator.generate(PWD_LENGTH);
		int affected = tbm02Mapper.updateEntryPwd(SessionSinglePwdCommand.of(
				param.sessionCd(), entryPwd, param.gvCmpnyCd(), param.gvUserCd()));
		if (affected == 0) {
			log.warn("TBM 입실비번 재발급 경합/상태부적합 - sessionCd={}", param.sessionCd());
			throw new ApiException(TbmErrorCode.TBM_409_012);
		}

		log.info("TBM 세션 입실비번 재발급 완료 - sessionCd={}", param.sessionCd());

		return SessionPwdResponse.builder()
				.sessionCd(param.sessionCd())
				.entryPwd(entryPwd)
				.exitPwd(null)
				.build();
	}

	// ============================ 관리자 대리/검색 입실 (prafta-051-11) ============================

	/**
	 * 입실 후보 검색. 세션을 먼저 조회·권한·스코프 검증(cross-site IDOR 차단)한 뒤, 세션 사업장
	 * (guard.siteCd) 기준으로 정규직/일용직 후보를 조회한다. 클라이언트가 보낸 사업장은 신뢰하지 않는다.
	 *
	 * <p>일용직은 C7(USE_YN='Y' + ACCOUNT_STATUS='01' + WITHDRAWAL_DATE IS NULL +
	 * WORK_EXPIRE_DATE&gt;=오늘) 만 노출하며 PII 는 끝 4자리만 포함한다.
	 */
	@Override
	public EntryCandidateResponse selectEntryCandidates(EntryCandidateParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		String userTypeCd = normalizeUserType(param.userTypeCd());

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		EntryCandidateQuery query = EntryCandidateQuery.of(param, guard.siteCd());

		List<EntryCandidateResult> list = "DAILY".equals(userTypeCd)
				? tbm02Mapper.selectDailyCandidates(query)
				: tbm02Mapper.selectRegularCandidates(query);

		log.info("TBM 입실 후보 검색 완료 - sessionCd={}, userType={}, count={}",
				param.sessionCd(), userTypeCd, list != null ? list.size() : 0);

		return EntryCandidateResponse.builder()
				.userTypeCd(userTypeCd)
				.candidateList(list != null ? list : Collections.emptyList())
				.build();
	}

	/**
	 * 관리자 직접 입실(MANAGER_DIRECT). OPENED 상태만 허용하며, 대상이 세션 사업장 소속 활성 사용자
	 * (일용직은 C7 만료/탈퇴 재확인)인지 서버에서 재검증한다.
	 *
	 * <p>재입실 처리(UK_TBM_ATTENDANCE_01 는 DEL_YN 미포함): 동일 슬롯에
	 * <ul>
	 *   <li>DEL_YN='N' 행 존재 → 이미 입실(TBM_409_041, 멱등 안내)</li>
	 *   <li>DEL_YN='Y' 행 존재 → 내보내기 후 재입실: 해당 행 RESTORE(UNIQUE 충돌 회피)</li>
	 *   <li>행 없음 → 신규 INSERT. INSERT 직전 동시 입실 경합은 DuplicateKeyException 으로 안내 처리</li>
	 * </ul>
	 */
	@Override
	@Transactional
	public ManagerEnterResponse managerEnter(ManagerEnterParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.userCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		String userTypeCd = normalizeUserType(param.userTypeCd());

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		// 교육준비(OPENED) 상태에서만 입실 처리(C)
		if (!"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 대리입실 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_040);
		}

		// 대상이 세션 사업장 소속 활성 사용자인지 재검증(일용직은 C7 만료/탈퇴 차단)
		int valid = tbm02Mapper.countEntryTarget(
				new EntryTargetQuery(param.gvCmpnyCd(), guard.siteCd(), userTypeCd, param.userCd()));
		if (valid <= 0) {
			log.warn("TBM 대리입실 대상 부적합 - sessionCd={}, userType={}, userCd={}, site={}",
					param.sessionCd(), userTypeCd, param.userCd(), guard.siteCd());
			throw new ApiException(TbmErrorCode.TBM_403_040);
		}

		ManagerEnterCommand command = new ManagerEnterCommand(
				param.gvCmpnyCd(), param.sessionCd(), userTypeCd, param.userCd(), param.gvUserCd());

		// 슬롯(UNIQUE 키) 점유 확인 후 INSERT/RESTORE 분기
		AttendanceSlotResult slot = tbm02Mapper.selectAttendanceSlot(command);
		boolean restored;
		if (slot == null) {
			try {
				tbm02Mapper.insertManagerEntry(command);
				restored = false;
			} catch (DuplicateKeyException e) {
				// 조회~INSERT 사이 동시 입실 경합(이미 입실)
				log.warn("TBM 대리입실 UNIQUE 충돌(동시 입실) - sessionCd={}, userCd={}",
						param.sessionCd(), param.userCd());
				throw new ApiException(TbmErrorCode.TBM_409_041);
			}
		} else if ("N".equals(slot.delYn())) {
			// 이미 입실 처리됨(멱등 안내)
			log.info("TBM 대리입실 멱등 - 이미 입실됨 sessionCd={}, userCd={}", param.sessionCd(), param.userCd());
			throw new ApiException(TbmErrorCode.TBM_409_041);
		} else {
			// 내보내기 후 재입실: DEL_YN='Y' 행 RESTORE
			int affected = tbm02Mapper.restoreManagerEntry(command);
			if (affected == 0) {
				// 경합으로 슬롯 상태 변경됨
				log.warn("TBM 대리입실 RESTORE 경합 - sessionCd={}, userCd={}", param.sessionCd(), param.userCd());
				throw new ApiException(TbmErrorCode.TBM_409_041);
			}
			restored = true;
		}

		log.info("TBM 대리입실 완료 - sessionCd={}, userType={}, userCd={}, manager={}, restored={}",
				param.sessionCd(), userTypeCd, param.userCd(), param.gvUserCd(), restored);

		return ManagerEnterResponse.builder()
				.sessionCd(param.sessionCd())
				.userTypeCd(userTypeCd)
				.userCd(param.userCd())
				.restored(restored)
				.build();
	}

	// ============================ 입실자 명단/내보내기 (prafta-051-12) ============================

	/**
	 * 교육준비 단계 입실자 명단(거리/입실유형 포함). 권한 + 세션 스코프 검증 후 DEL_YN='N' 입실자만
	 * 조회한다. tbm04 와 달리 OPENED 단계에서 호출되므로 tbm02 전용 쿼리로 권한/상태 일관성을 유지한다.
	 */
	@Override
	public SessionAttendanceListResponse selectSessionAttendances(SessionDetailParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		List<SessionAttendanceResult> list = tbm02Mapper.selectSessionAttendances(
				new SessionDetailQuery(param.sessionCd(), param.gvCmpnyCd()));

		log.info("TBM 입실자 명단 조회 완료 - sessionCd={}, count={}",
				param.sessionCd(), list != null ? list.size() : 0);

		return SessionAttendanceListResponse.builder()
				.sessionCd(param.sessionCd())
				.totalCount(list != null ? list.size() : 0)
				.attendanceList(list != null ? list : Collections.emptyList())
				.build();
	}

	/**
	 * 입실자 내보내기(soft delete). 교육준비(OPENED) 상태만 허용(C8). 사유 필수. 대상 출결이 해당
	 * 세션 소속이고 DEL_YN='N' 인 행만 갱신하며, 영향행 0 이면 이미 제거/세션 불일치로 거부한다.
	 *
	 * <p>UK_TBM_ATTENDANCE_01 는 DEL_YN 미포함이므로 내보낸 사용자의 재입실은 managerEnter 가
	 * 기존 행 RESTORE 로 처리한다(UNIQUE 충돌 회피).
	 */
	@Override
	@Transactional
	public void ejectAttendance(EjectAttendanceParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd()) || !StringUtils.hasText(param.attendanceCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		if (!StringUtils.hasText(param.reason())) {
			throw new ApiException(TbmErrorCode.TBM_400_041);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		// 교육준비(OPENED) 상태에서만 내보내기(C8)
		if (!"OPENED".equals(guard.statusCd())) {
			log.warn("TBM 입실자 내보내기 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_042);
		}

		int affected = tbm02Mapper.ejectAttendance(EjectAttendanceCommand.of(param));
		if (affected == 0) {
			// 세션 불일치 / 이미 제거 / 부적합
			log.warn("TBM 입실자 내보내기 대상 없음/부적합 - sessionCd={}, attendanceCd={}",
					param.sessionCd(), param.attendanceCd());
			throw new ApiException(TbmErrorCode.TBM_409_043);
		}

		log.info("TBM 입실자 내보내기 완료 - sessionCd={}, attendanceCd={}, manager={}",
				param.sessionCd(), param.attendanceCd(), param.gvUserCd());
	}

	// ============================ 보조 조회 ============================

	@Override
	public ContentOptionResponse selectContentOptions(OptionParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}
		OptionQuery query = OptionQuery.from(param);
		return ContentOptionResponse.builder()
				.contentList(tbm02Mapper.selectContentOptions(query))
				.build();
	}

	@Override
	public RiskOptionResponse selectRiskOptions(OptionParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}
		OptionQuery query = OptionQuery.from(param);
		List<RiskOptionResult> list = tbm02Mapper.selectRiskOptions(query);

		// [Medium 보강/T6] risk-options 는 엔드포인트에 관리자 게이트가 없고 999999 만 차단하므로,
		// 비관리자(일반 근로자) 호출 시 평가자 실명(FNC 복호화)이 노출된다. session-detail 과 동일 출처
		// (서버 권위 authCd) 게이트로 비관리자에게는 평가요청자 실명을 redaction 한다. 개설은 관리자 행위라
		// 관리자(master/safe/hr)는 그대로 노출되어 개설 흐름 무영향.
		boolean assessorVisible = isManagerRole(param.gvAuthCd());

		List<RiskOptionResponse.RiskOptionItem> items = new ArrayList<>();
		if (list != null) {
			for (RiskOptionResult r : list) {
				items.add(RiskOptionResponse.RiskOptionItem.builder()
						.siteCd(r.siteCd())
						.processCd(r.processCd())
						.processNm(r.processNm())
						.riskTypeCd(r.riskTypeCd())
						.riskTypeNm(r.riskTypeNm())
						.hazardCd(r.hazardCd())
						.hazardNm(r.hazardNm())
						.assessmentCd(r.assessmentCd())
						.assessmentStatus(r.assessmentStatus())
						.assessmentStatusNm(r.assessmentStatusNm())
						.displayName(buildRiskDisplayName(r.processNm(), r.riskTypeNm(), r.hazardNm()))
						// 6.3(T6-13): 위험성평가 선택 팝업 컬럼 — 평가요청일(비민감)/평가요청자(관리자만)
						.initAssessDate(r.initAssessDate())
						.initAssessorNm(assessorVisible ? r.initAssessorNm() : null)
						.build());
			}
		}

		return RiskOptionResponse.builder()
				.riskList(items)
				.build();
	}

	@Override
	public SiteOptionResponse selectSiteOptions(OptionParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}
		return SiteOptionResponse.builder()
				.siteList(tbm02Mapper.selectSiteOptions(param.gvCmpnyCd()))
				.build();
	}

	// ============================ 내부 헬퍼 ============================

	/** 콘텐츠 매핑 다건 INSERT(DISPLAY_ORDER 자동 부여). */
	private void insertContents(List<SessionContentModel> contents, String sessionCd,
			String gvCmpnyCd, String gvUserCd) {
		if (contents == null) {
			return;
		}
		int order = 0;
		for (SessionContentModel model : contents) {
			if (!StringUtils.hasText(model.getMtrlCd())) {
				continue;
			}
			tbm02Mapper.insertSessionContent(
					SessionContentCommand.from(model, sessionCd, order, gvCmpnyCd, gvUserCd));
			order++;
		}
	}

	/** 위험성평가 매핑 다건 INSERT(DISPLAY_ORDER 자동 부여, 옵션). */
	private void insertRisks(List<SessionRiskModel> risks, String sessionCd,
			String gvCmpnyCd, String gvUserCd) {
		if (risks == null) {
			return;
		}
		int order = 0;
		for (SessionRiskModel model : risks) {
			if (!StringUtils.hasText(model.getProcessCd()) || !StringUtils.hasText(model.getAssessmentCd())) {
				continue;
			}
			tbm02Mapper.insertSessionRisk(
					SessionRiskCommand.from(model, sessionCd, order, gvCmpnyCd, gvUserCd));
			order++;
		}
	}

	/**
	 * 단순 상태 전이(start/extend/complete/regenerate-exit) 공통 가드:
	 * 권한 + sessionCd 필수 + 세션 조회 + 스코프 격리를 한 번에 수행하고 가드를 반환한다.
	 */
	private SessionGuardResult loadTransitionGuard(SessionTransitionParam param) {
		verifyManageAuth(param.gvAuthCd());

		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}

		SessionGuardResult guard = loadGuard(param.gvCmpnyCd(), param.sessionCd());
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());
		return guard;
	}

	/** 게이트 검증용 세션 조회(없으면 404). */
	private SessionGuardResult loadGuard(String gvCmpnyCd, String sessionCd) {
		SessionGuardResult guard = tbm02Mapper.selectSessionGuard(
				new SessionDetailQuery(sessionCd, gvCmpnyCd));
		if (guard == null) {
			throw new ApiException(TbmErrorCode.TBM_404_010);
		}
		return guard;
	}

	/** 대상유형 정규화/검증(REGULAR|DAILY 외 거부). 미입력 시 REGULAR 기본 아님 - 명시 강제. */
	private String normalizeUserType(String userTypeCd) {
		if (!"REGULAR".equals(userTypeCd) && !"DAILY".equals(userTypeCd)) {
			log.warn("TBM 입실 대상유형 부적합 - userTypeCd={}", userTypeCd);
			throw new ApiException(TbmErrorCode.TBM_400_040);
		}
		return userTypeCd;
	}

	/** 개설/수정/취소/재발급 권한 게이트: 회사 전체 권한(master/safe)만 허용. */
	private void verifyManageAuth(String authCd) {
		if (AuthRoleUtils.isAccessDenied(authCd)) {
			log.warn("TBM 세션 관리 접근 차단 - authCd={}", authCd);
			throw new ApiException(TbmErrorCode.TBM_403_001);
		}
		// 개설=safe + 회사 전체 권한(master/safe). 회사별 커스텀 권한 확장은 후속.
		if (!AuthRoleUtils.canManageCommon(authCd)) {
			log.warn("TBM 세션 관리 권한 없음 - authCd={}", authCd);
			throw new ApiException(TbmErrorCode.TBM_403_010);
		}
	}

	/** 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 세션만 접근 가능. */
	private void verifyScope(String authCd, String ownSiteCd, String targetSiteCd) {
		if (AuthRoleUtils.isCompanyWide(authCd)) {
			return;
		}
		if (ownSiteCd == null || !ownSiteCd.equals(targetSiteCd)) {
			log.warn("TBM 세션 스코프 위반 - authCd={}, ownSite={}, targetSite={}",
					authCd, ownSiteCd, targetSiteCd);
			throw new ApiException(TbmErrorCode.TBM_403_011);
		}
	}

	/**
	 * 입실비밀번호 노출 게이트(T6-03): 교육준비(OPENED)/교육시작(IN_PROGRESS) + 관리자.
	 * 입실비번은 입실 마감 전(OPENED/IN_PROGRESS)에만 의미가 있으므로 종료 후에는 숨긴다.
	 */
	private boolean isEntryPwdVisible(String statusCd, String authCd) {
		boolean openStatus = "OPENED".equals(statusCd) || "IN_PROGRESS".equals(statusCd);
		return openStatus && isManagerRole(authCd);
	}

	/**
	 * 종료비밀번호 노출 게이트(T6-03): 교육종료(COMPLETED) + 관리자.
	 * 종료비번은 교육종료 전이 시 발급되므로 COMPLETED 에서 관리자에게 상시 노출한다
	 * (재발급 후 fnSearch 가 null 로 덮어쓰던 6.2-(1)-3 결함 해소).
	 */
	private boolean isExitPwdVisible(String statusCd, String authCd) {
		return "COMPLETED".equals(statusCd) && isManagerRole(authCd);
	}

	/** 비밀번호 열람 가능 관리자 역할(safe/master/hr). */
	private boolean isManagerRole(String authCd) {
		return AuthRoleUtils.canManageCommon(authCd) || AuthRoleUtils.isManager(authCd);
	}

	/**
	 * 교육 인정시간(분) 검증(공유계약: 1~60 정수).
	 *
	 * <p>{@code required=false}: null 허용(개설/수정/임시저장). 값이 있으면 1~60 검증.
	 * {@code required=true}: null도 거부(교육준비 시작 — 미입력이면 TBM_400_015).
	 * 웹/앱 공용 단일 규칙(이원화 금지). 범위밖/미입력 모두 TBM_400_015 로 안내.
	 */
	private void validateEduMinutes(Integer eduMinutes, boolean required) {
		if (eduMinutes == null) {
			if (required) {
				log.warn("TBM 교육시간 미입력(교육준비 필수)");
				throw new ApiException(TbmErrorCode.TBM_400_015);
			}
			return;
		}
		if (eduMinutes < EDU_MINUTES_MIN || eduMinutes > EDU_MINUTES_MAX) {
			log.warn("TBM 교육시간 범위 벗어남 - eduMinutes={}", eduMinutes);
			throw new ApiException(TbmErrorCode.TBM_400_015);
		}
	}

	/** 교육 내용(리치 HTML) 텍스트 길이 검증(빈 HTML 거부). */
	private void validateContentBody(String contentBody) {
		String text = stripHtml(contentBody);
		if (text.length() < CONTENT_TEXT_MIN) {
			log.warn("TBM 교육 내용 텍스트 부족 - len={}", text.length());
			throw new ApiException(TbmErrorCode.TBM_400_010);
		}
	}

	/** HTML 태그/엔티티/공백 제거 후 순수 텍스트 추출. */
	private String stripHtml(String html) {
		if (html == null) {
			return "";
		}
		String text = html.replaceAll("(?is)<[^>]*>", "");	// 태그 제거
		text = text.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&lt;", "<")
				.replace("&gt;", ">");
		return text.replaceAll("\\s+", "").trim();
	}

	/** GPS 검증 설정 검증(05_02 §2.2). */
	private void validateGps(String gpsVerifyTypeCd, String lat, String lon,
			Integer radiusM, String manualConfirmYn) {
		String type = StringUtils.hasText(gpsVerifyTypeCd) ? gpsVerifyTypeCd : "AUTO";

		if (!"AUTO".equals(type) && !"MANUAL".equals(type) && !"DISABLED".equals(type)) {
			throw new ApiException(TbmErrorCode.TBM_400_012);
		}

		if ("AUTO".equals(type)) {
			if (!StringUtils.hasText(lat) || !StringUtils.hasText(lon)) {
				log.warn("TBM GPS AUTO 좌표 누락");
				throw new ApiException(TbmErrorCode.TBM_400_012);
			}
		} else if ("MANUAL".equals(type)) {
			if (!"Y".equals(manualConfirmYn)) {
				log.warn("TBM GPS MANUAL 확인 누락");
				throw new ApiException(TbmErrorCode.TBM_400_012);
			}
		}

		// 반경 검증(미입력 시 기본값 허용). 입력 시 50~1000 범위
		if (radiusM != null && (radiusM < GPS_RADIUS_MIN || radiusM > GPS_RADIUS_MAX)) {
			throw new ApiException(TbmErrorCode.TBM_400_013);
		}
	}

	/**
	 * 세션 상세 위험성평가 항목 빌드(T6 + Medium 보강). 비민감 요약(분류 코드/표시명/상태/평가요청일)은
	 * 상시 노출하고, 평가자 실명·파일경로·점수·설명 등 민감 상세 블록은 {@code detailVisible}
	 * (서버 권위 authCd 기반 관리자 판정)일 때만 채운다. 비관리자는 해당 필드가 null 로 redaction 된다.
	 *
	 * <p>관리자(master/safe/hr)는 기존과 동일 응답(콘솔/RiskAssessInfo 읽기전용 열람 무영향).
	 */
	private SessionDetailResponse.SessionRiskItem buildSessionRiskItem(SessionRiskResult r, boolean detailVisible) {
		SessionDetailResponse.SessionRiskItem.SessionRiskItemBuilder builder =
				SessionDetailResponse.SessionRiskItem.builder()
						// 비민감 요약(상시 노출): 분류 코드/명 + 표시명 + 상태 + 평가요청일
						.cmpnyCd(r.cmpnyCd())
						.siteCd(r.siteCd())
						.processCd(r.processCd())
						.processNm(r.processNm())
						.riskTypeCd(r.riskTypeCd())
						.riskTypeNm(r.riskTypeNm())
						.hazardCd(r.hazardCd())
						.hazardNm(r.hazardNm())
						.assessmentCd(r.assessmentCd())
						.assessmentStatus(r.assessmentStatus())
						.assessmentStatusNm(r.assessmentStatusNm())
						.displayName(buildRiskDisplayName(r.processNm(), r.riskTypeNm(), r.hazardNm()))
						.displayOrder(r.displayOrder())
						// 6.2-(1)-2: 평가요청일(비민감 요약, 상시 노출)
						.initAssessDate(r.initAssessDate());

		if (detailVisible) {
			// 관리자 전용: 평가자 실명/파일경로/점수·설명 상세(RiskAssessInfo 읽기전용 팝업 채움용)
			builder.initAssessorNm(r.initAssessorNm())
					.initLikelihoodScore(r.initLikelihoodScore())
					.initSeverityScore(r.initSeverityScore())
					.initRiskLv(r.initRiskLv())
					.initDesc(r.initDesc())
					.initAssessorId(r.initAssessorId())
					.initFileMgmtCd(r.initFileMgmtCd())
					.initFilePath(r.initFilePath())
					.revalDate(r.revalDate())
					.revalBeforeDesc(r.revalBeforeDesc())
					.revalLikelihoodScore(r.revalLikelihoodScore())
					.revalSeverityScore(r.revalSeverityScore())
					.revalRiskLv(r.revalRiskLv())
					.revalDesc(r.revalDesc())
					.revalAssessorId(r.revalAssessorId())
					.revalAssessorNm(r.revalAssessorNm())
					.revalAssessDate(r.revalAssessDate())
					.revalFileMgmtCd(r.revalFileMgmtCd())
					.revalFilePath(r.revalFilePath());
		}
		// 비관리자: 위 민감 필드는 builder 미설정(=null) 으로 redaction.

		return builder.build();
	}

	/**
	 * 위험성평가 표시명 합성(plan §8-1). Risk03 규약 재사용:
	 * 공정명 / 위험요인구분명 / 유해요인명을 ' &gt; '로 연결. 모두 비어 있으면 빈 문자열.
	 */
	private String buildRiskDisplayName(String processNm, String riskTypeNm, String hazardNm) {
		List<String> parts = new ArrayList<>();
		if (StringUtils.hasText(processNm)) {
			parts.add(processNm);
		}
		if (StringUtils.hasText(riskTypeNm)) {
			parts.add(riskTypeNm);
		}
		if (StringUtils.hasText(hazardNm)) {
			parts.add(hazardNm);
		}
		return String.join(" > ", parts);
	}
}
