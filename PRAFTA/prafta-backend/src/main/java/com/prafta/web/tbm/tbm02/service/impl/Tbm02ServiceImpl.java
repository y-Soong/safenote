package com.prafta.web.tbm.tbm02.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.NumericPwdGenerator;
import com.prafta.web.tbm.tbm02.application.command.SessionCancelCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionContentCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionPwdCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionRiskCommand;
import com.prafta.web.tbm.tbm02.application.command.SessionStateCommand;
import com.prafta.web.tbm.tbm02.application.model.SessionContentModel;
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;
import com.prafta.web.tbm.tbm02.application.param.OptionParam;
import com.prafta.web.tbm.tbm02.application.param.SessionCancelParam;
import com.prafta.web.tbm.tbm02.application.param.SessionDetailParam;
import com.prafta.web.tbm.tbm02.application.param.SessionListParam;
import com.prafta.web.tbm.tbm02.application.param.SessionPwdParam;
import com.prafta.web.tbm.tbm02.application.param.SessionSaveParam;
import com.prafta.web.tbm.tbm02.application.param.SessionUpdateParam;
import com.prafta.web.tbm.tbm02.application.query.OptionQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionDetailQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionListQuery;
import com.prafta.web.tbm.tbm02.dto.response.ContentOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.RiskOptionResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionDetailResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionListResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionPwdResponse;
import com.prafta.web.tbm.tbm02.dto.response.SessionSaveResponse;
import com.prafta.web.tbm.tbm02.dto.response.SiteOptionResponse;
import com.prafta.web.tbm.tbm02.mapper.Tbm02Mapper;
import com.prafta.web.tbm.tbm02.result.RiskOptionResult;
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

	/** 비밀번호 자리수(랜덤 6자리). */
	private static final int PWD_LENGTH = 6;

	/** GPS 검증 반경 범위(05_02 §2.2). */
	private static final int GPS_RADIUS_MIN = 50;
	private static final int GPS_RADIUS_MAX = 1000;

	/** 교육 내용 최소 텍스트 길이(빈 HTML 거부). */
	private static final int CONTENT_TEXT_MIN = 10;

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

		// 비밀번호 노출 게이트: OPENED/IN_PROGRESS 상태 + 관리자(safe/master/hr)만
		boolean pwdVisible = isPwdVisible(session.statusCd(), param.gvAuthCd());

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
				.entryPwd(pwdVisible ? session.entryPwd() : null)
				.exitPwd(pwdVisible ? session.exitPwd() : null)
				.pwdVisible(pwdVisible)
				.managerUserCd(session.managerUserCd())
				.managerUserNm(session.managerUserNm())
				.managerGpsLat(session.managerGpsLat())
				.managerGpsLon(session.managerGpsLon())
				.gpsVerifyTypeCd(session.gpsVerifyTypeCd())
				.gpsVerifyRadiusM(session.gpsVerifyRadiusM())
				.gpsManualConfirmYn(session.gpsManualConfirmYn())
				.openedAt(session.openedAt())
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
				riskItems.add(SessionDetailResponse.SessionRiskItem.builder()
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
						.build());
			}
		}

		log.info("TBM 세션 상세 조회 완료 - sessionCd={}, status={}, pwdVisible={}",
				session.sessionCd(), session.statusCd(), pwdVisible);

		return SessionDetailResponse.builder()
				.session(item)
				.contents(contents != null ? contents : Collections.emptyList())
				.risks(riskItems)
				.build();
	}

	// ============================ W-05 개설/임시저장 ============================

	@Override
	@Transactional
	public SessionSaveResponse saveSession(SessionSaveParam param) {
		// 권한 게이트: 개설=safe + 회사 전체 권한(master/safe). 999999 차단.
		verifyManageAuth(param.gvAuthCd());

		boolean opened = "OPENED".equals(param.saveMode());
		// saveMode 검증: OPENED / DRAFT 만 허용
		if (!opened && !"DRAFT".equals(param.saveMode())) {
			log.warn("TBM 세션 저장 모드 부적합 - saveMode={}", param.saveMode());
			throw new ApiException(CommonErrorCode.COMMON_400_002);
		}

		// 사업장 필수 + 스코프 격리(회사 전체 권한이 아니면 자기 사업장만)
		if (!StringUtils.hasText(param.siteCd())) {
			throw new ApiException(CommonErrorCode.COMMON_400_001);
		}
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

		// 제목 검증
		String title = param.title() != null ? param.title().trim() : "";
		if (!StringUtils.hasText(title) || title.length() > 200) {
			throw new ApiException(TbmErrorCode.TBM_400_011);
		}

		// 개설 시: 교육 내용 텍스트 >= 10자, GPS 검증
		if (opened) {
			validateContentBody(param.contentBody());
			validateGps(param.gpsVerifyTypeCd(), param.managerGpsLat(), param.managerGpsLon(),
					param.gpsVerifyRadiusM(), param.gpsManualConfirmYn());
		}

		String sessionCd = tbm02Mapper.selectSessionCd(param.gvCmpnyCd());
		if (!StringUtils.hasText(sessionCd)) {
			log.error("TBM 세션코드 채번 실패 - cmpnyCd={}", param.gvCmpnyCd());
			throw new ApiException(CommonErrorCode.COMMON_500_001);
		}

		String entryPwd = null;
		String exitPwd = null;
		if (opened) {
			String[] pair = NumericPwdGenerator.generatePair(PWD_LENGTH);
			entryPwd = pair[0];
			exitPwd = pair[1];
		}

		String statusCd = opened ? "OPENED" : "DRAFT";

		// 1. 세션 헤더 INSERT
		tbm02Mapper.insertSession(SessionCommand.forSave(param, sessionCd, statusCd, entryPwd, exitPwd, opened));

		// 2. 콘텐츠 매핑 INSERT
		insertContents(param.contents(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());

		// 3. 위험성평가 매핑 INSERT(옵션)
		insertRisks(param.risks(), sessionCd, param.gvCmpnyCd(), param.gvUserCd());

		// 4. 개설 시 상태 row 초기 UPSERT(C 단계가 사용할 동기화 상태 초기값)
		if (opened) {
			tbm02Mapper.upsertSessionState(SessionStateCommand.of(sessionCd, param.gvCmpnyCd(), param.gvUserCd()));
		}

		boolean riskEmpty = param.risks() == null || param.risks().isEmpty();

		log.info("TBM 세션 저장 완료 - sessionCd={}, status={}, riskEmpty={}", sessionCd, statusCd, riskEmpty);

		return SessionSaveResponse.builder()
				.sessionCd(sessionCd)
				.statusCd(statusCd)
				.entryPwd(entryPwd)
				.exitPwd(exitPwd)
				.warningMessage(riskEmpty ? RISK_WARNING_MESSAGE : null)
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
			log.warn("TBM 세션 비번 재발급 불가 상태 - sessionCd={}, status={}", param.sessionCd(), guard.statusCd());
			throw new ApiException(TbmErrorCode.TBM_409_012);
		}

		String[] pair = NumericPwdGenerator.generatePair(PWD_LENGTH);
		tbm02Mapper.updateSessionPwd(SessionPwdCommand.of(
				param.sessionCd(), pair[0], pair[1], param.gvCmpnyCd(), param.gvUserCd()));

		log.info("TBM 세션 비번 재발급 완료 - sessionCd={}", param.sessionCd());

		return SessionPwdResponse.builder()
				.sessionCd(param.sessionCd())
				.entryPwd(pair[0])
				.exitPwd(pair[1])
				.build();
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

	/** 게이트 검증용 세션 조회(없으면 404). */
	private SessionGuardResult loadGuard(String gvCmpnyCd, String sessionCd) {
		SessionGuardResult guard = tbm02Mapper.selectSessionGuard(
				new SessionDetailQuery(sessionCd, gvCmpnyCd));
		if (guard == null) {
			throw new ApiException(TbmErrorCode.TBM_404_010);
		}
		return guard;
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

	/** 비밀번호 노출 게이트: OPENED/IN_PROGRESS 상태 + 관리자(safe/master/hr). */
	private boolean isPwdVisible(String statusCd, String authCd) {
		boolean openStatus = "OPENED".equals(statusCd) || "IN_PROGRESS".equals(statusCd);
		boolean isManager = AuthRoleUtils.canManageCommon(authCd) || AuthRoleUtils.isManager(authCd);
		return openStatus && isManager;
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
