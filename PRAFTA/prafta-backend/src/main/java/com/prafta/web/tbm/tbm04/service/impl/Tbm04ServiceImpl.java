package com.prafta.web.tbm.tbm04.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.tbmshare.result.TbmSessionAccess;
import com.prafta.common.cmm.tbmshare.service.TbmSessionShareService;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm04.application.command.CompletionUpdateCommand;
import com.prafta.web.tbm.tbm04.application.param.AttendanceEventParam;
import com.prafta.web.tbm.tbm04.application.param.CompletionUpdateParam;
import com.prafta.web.tbm.tbm04.application.param.EvidenceListParam;
import com.prafta.web.tbm.tbm04.application.param.HistorySessionListParam;
import com.prafta.web.tbm.tbm04.application.param.SessionAttendanceParam;
import com.prafta.web.tbm.tbm04.application.param.UserAttendanceParam;
import com.prafta.web.tbm.tbm04.application.query.AttendanceEventQuery;
import com.prafta.web.tbm.tbm04.application.query.AttendanceGuardQuery;
import com.prafta.web.tbm.tbm04.application.query.EvidenceQuery;
import com.prafta.web.tbm.tbm04.application.query.HistorySessionListQuery;
import com.prafta.web.tbm.tbm04.application.query.SessionAttendanceQuery;
import com.prafta.web.tbm.tbm04.application.query.UserAttendanceQuery;
import com.prafta.web.tbm.tbm04.dto.response.AttendanceEventResponse;
import com.prafta.web.tbm.tbm04.dto.response.EvidenceSessionDetailResponse;
import com.prafta.web.tbm.tbm04.dto.response.EvidenceSessionListResponse;
import com.prafta.web.tbm.tbm04.dto.response.EvidenceWorkerSummaryResponse;
import com.prafta.web.tbm.tbm04.dto.response.HistorySessionListResponse;
import com.prafta.web.tbm.tbm04.dto.response.SessionAttendanceResponse;
import com.prafta.web.tbm.tbm04.dto.response.UserAttendanceResponse;
import com.prafta.web.tbm.tbm04.mapper.Tbm04Mapper;
import com.prafta.web.tbm.tbm04.result.AttendanceEventResult;
import com.prafta.web.tbm.tbm04.result.AttendanceGuardResult;
import com.prafta.web.tbm.tbm04.result.HistorySessionListResult;
import com.prafta.web.tbm.tbm04.result.HistoryStatResult;
import com.prafta.web.tbm.tbm04.result.SessionAttendanceResult;
import com.prafta.web.tbm.tbm04.result.UserAttendanceResult;
import com.prafta.web.tbm.tbm04.result.UserAttendanceSummaryResult;
import com.prafta.web.tbm.tbm04.result.UserInfoResult;
import com.prafta.web.tbm.tbm04.service.Tbm04Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 이력 관리(W-12~15). prafta-033-D.
 *
 * <p>출결/이벤트 테이블은 읽기 전용 + W-14 사후 보정 UPDATE 만 다룬다.
 * 입실/종료/QR/SSE 쓰기는 C·앱 소관. 출결 데이터가 비어 있어도 모든 조회/통계는 빈 상태로 정상 동작한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Tbm04ServiceImpl implements Tbm04Service {

	private final Tbm04Mapper tbm04Mapper;
	/** PRAFTA-SUBCON-T5: 연동 회사 지정 공통 검증 지점(세션 소유 검증/relabel). */
	private final TbmSessionShareService tbmSessionShareService;
	/** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
	private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;
	/** W-13 확장 — 출결 서명 이미지 스트림(공용 파일 로더). */
	private final com.prafta.common.cmm.file.service.FileService fileService;

	/** 미이수 처리 사유 최소 길이(D 문서 §4.1). */
	private static final int REASON_MIN = 10;

	/** 이상신호 레벨(클라 계산 금지 — 백엔드 산출). */
	private static final String ANOMALY_NONE = "NONE";
	private static final String ANOMALY_LOW = "LOW";
	private static final String ANOMALY_HIGH = "HIGH";

	// ============================ W-12 이력 목록 ============================

	@Override
	public HistorySessionListResponse selectHistorySessionList(HistorySessionListParam param) {
		// 999999(권한 미부여)는 화면 진입 차단
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 이력 목록 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}

		HistorySessionListQuery query = HistorySessionListQuery.from(param);

		List<HistorySessionListResult> list = tbm04Mapper.selectHistorySessionList(query);
		int totalCount = tbm04Mapper.selectHistorySessionListCount(query);
		HistoryStatResult stat = tbm04Mapper.selectHistoryStat(query);

		HistorySessionListResponse.StatSummary statSummary = buildStatSummary(stat);

		log.info("TBM 이력 목록 조회 완료 - cmpnyCd={}, count={}, totalCount={}",
				param.gvCmpnyCd(), list != null ? list.size() : 0, totalCount);

		return HistorySessionListResponse.builder()
				.historyList(list != null ? list : Collections.emptyList())
				.totalCount(totalCount)
				.page(param.page())
				.pageSize(param.pageSize())
				.stat(statSummary)
				.build();
	}

	// ============================ W-13 출결 명단 ============================

	@Override
	public SessionAttendanceResponse selectSessionAttendances(SessionAttendanceParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 출결 명단 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
		if (!StringUtils.hasText(param.sessionCd())) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		// ⚠ PRAFTA-SUBCON-T5(보안): 기존 코드에는 세션 소유 검증이 없었고 테넌트 격리를 매퍼의
		//   AT.CMPNY_CD = gvCmpnyCd 조건에만 의존했다. T5 가 출결 스코프를 SESSION_CD 단독으로
		//   넓히므로, 스코프 확대 전에 "세션이 내 회사(개설사) 소유"임을 반드시 선검증한다.
		//   (검증 없이 넓히면 즉시 IDOR — 타사 세션의 출결 명단이 열린다.)
		// 웹 관리자 경로는 grandfather 를 적용하지 않는다(개설사 소유 검증이 목적) → 사용자 식별자 null.
		TbmSessionAccess access = tbmSessionShareService.assertViewable(
				param.sessionCd(), param.gvCmpnyCd(), null, null, null);
		if (!access.owner()) {
			log.warn("TBM 출결 명단 접근 차단(개설사 아님) - sessionCd={}, cmpnyCd={}",
					param.sessionCd(), param.gvCmpnyCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
		// 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 세션 출결만.
		verifyScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), access.session().siteCd());

		SessionAttendanceQuery query = SessionAttendanceQuery.from(param);

		List<SessionAttendanceResult> list = tbm04Mapper.selectSessionAttendances(query);

		// 타사 참석자 소속 relabel(개설사 직하 1차 회사명. 2차 이하 회사명/코드는 응답에 없음).
		Map<String, String> labels = tbmSessionShareService.resolveTier1LabelMap(param.sessionCd());

		List<SessionAttendanceResponse.AttendanceItem> items = new ArrayList<>();
		int completedCount = 0;
		int notCompletedCount = 0;

		String hostCmpnyCd = access.session().hostCmpnyCd();

		if (list != null) {
			for (SessionAttendanceResult r : list) {
				boolean exited = StringUtils.hasText(r.exitAt());
				// F7(최소 노출): 타사 참석자는 <b>성명만</b> 노출한다(승인 범위 D5/Q1).
				//   부서명(타사 조직구조)과 사내 사번(USER_CD)은 인접 차수 밖 정보이므로 redaction 한다.
				//   콘솔 액션은 전부 attendanceCd 로 동작하므로 기능 영향 없음.
				boolean foreign = r.cmpnyCd() != null && !r.cmpnyCd().equals(hostCmpnyCd);
				boolean forcedEnd = "MANAGER_FORCED".equals(r.exitTypeCd());
				String anomalyLevel = resolveAnomalyLevel(r.backgroundCount(),
						r.gpsOutOfRangeCount(), r.networkLostCount());

				if ("COMPLETED".equals(r.completionStatusCd())) {
					completedCount++;
				} else if ("NOT_COMPLETED".equals(r.completionStatusCd())) {
					notCompletedCount++;
				}

				items.add(SessionAttendanceResponse.AttendanceItem.builder()
						.attendanceCd(r.attendanceCd())
						.userTypeCd(r.userTypeCd())
						.userTypeNm(r.userTypeNm())
						.userCd(foreign ? null : r.userCd())
						.userNm(r.userNm())
						.deptNm(foreign ? null : r.deptNm())
						// F7-r: 타사 일용직의 휴대폰 끝4자리도 내리지 않는다. 개설사 콘솔의 식별은
						//   성명 + 소속(1차 relabel)로 충분하고, 출결 액션은 attendanceCd 기반이라 기능
						//   영향이 없다. 승인 범위(D5/Q1 = 성명만)를 넘지 않기 위한 최소 노출 원칙.
						.mblNoLast4(foreign ? null : r.mblNoLast4())
						.entryTypeCd(r.entryTypeCd())
						.entryAt(r.entryAt())
						.exitTypeCd(r.exitTypeCd())
						.exitAt(r.exitAt())
						.exited(exited)
						.exitForcedReason(r.exitForcedReason())
						.forcedEnd(forcedEnd)
						.entrySignFileMgmtCd(r.entrySignFileMgmtCd())
						.exitSignFileMgmtCd(r.exitSignFileMgmtCd())
						.completionStatusCd(r.completionStatusCd())
						.completionStatusNm(r.completionStatusNm())
						.notCompletedReason(r.notCompletedReason())
						.statusUpdatedBy(r.statusUpdatedBy())
						.statusUpdatedByNm(r.statusUpdatedByNm())
						.statusUpdatedAt(r.statusUpdatedAt())
						.appForegroundSec(r.appForegroundSec())
						.entryDistanceM(r.entryDistanceM())
						.backgroundCount(r.backgroundCount())
						.gpsOutOfRangeCount(r.gpsOutOfRangeCount())
						.networkLostCount(r.networkLostCount())
						.eventCount(r.eventCount())
						.anomalyLevel(anomalyLevel)
						.affilCmpnyNm(labels.get(r.cmpnyCd()))
						.build());
			}
		}

		log.info("TBM 출결 명단 조회 완료 - sessionCd={}, count={}, completed={}, notCompleted={}",
				param.sessionCd(), items.size(), completedCount, notCompletedCount);

		return SessionAttendanceResponse.builder()
				.attendanceList(items)
				.totalCount(items.size())
				.completedCount(completedCount)
				.notCompletedCount(notCompletedCount)
				.build();
	}

	// ============================ W-13 이벤트 타임라인 ============================

	@Override
	public AttendanceEventResponse selectAttendanceEvents(AttendanceEventParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 이벤트 타임라인 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
		if (!StringUtils.hasText(param.attendanceCd())) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		AttendanceEventQuery query = AttendanceEventQuery.from(param);

		List<AttendanceEventResult> list = tbm04Mapper.selectAttendanceEvents(query);
		int totalCount = tbm04Mapper.selectAttendanceEventCount(query);

		log.info("TBM 이벤트 타임라인 조회 완료 - attendanceCd={}, count={}, totalCount={}",
				param.attendanceCd(), list != null ? list.size() : 0, totalCount);

		return AttendanceEventResponse.builder()
				.eventList(list != null ? list : Collections.emptyList())
				.totalCount(totalCount)
				.page(param.page())
				.pageSize(param.pageSize())
				.build();
	}

	// ============================ W-14 미이수 처리 ============================

	@Override
	@Transactional
	public void updateCompletion(CompletionUpdateParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 미이수 처리 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_020);
		}
		if (!StringUtils.hasText(param.attendanceCd())) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		// 이수상태 코드 검증(COMPLETED / NOT_COMPLETED 만)
		String statusCd = param.completionStatusCd();
		if (!"COMPLETED".equals(statusCd) && !"NOT_COMPLETED".equals(statusCd)) {
			throw new ApiException(TbmErrorCode.TBM_400_020);
		}

		// 사유 필수 + 10자 이상(이수/미이수 모두 — 관리자 책임 기록)
		String reason = param.reason() != null ? param.reason().trim() : "";
		if (reason.length() < REASON_MIN) {
			throw new ApiException(TbmErrorCode.TBM_400_021);
		}

		// 출결 + 소속 세션 메타 조회(없으면 404)
		AttendanceGuardResult guard = tbm04Mapper.selectAttendanceGuard(
				new AttendanceGuardQuery(param.attendanceCd(), param.gvCmpnyCd()));
		if (guard == null) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		// 권한: 개설자 본인 / safe / master
		verifyCompletionAuth(param.gvAuthCd(), param.gvUserCd(), guard.managerUserCd());
		// 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 출결만
		verifyScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

		int affected = tbm04Mapper.updateCompletion(CompletionUpdateCommand.from(param));
		if (affected == 0) {
			// 동시성/삭제 등으로 대상 없음
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		log.info("TBM 미이수 처리 완료 - attendanceCd={}, prevStatus={}, newStatus={}, by={}",
				param.attendanceCd(), guard.completionStatusCd(), statusCd, param.gvUserCd());
	}

	// ============================ W-15 사용자별 이수 ============================

	@Override
	public UserAttendanceResponse selectUserAttendances(UserAttendanceParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 사용자별 이수 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
		if (!StringUtils.hasText(param.userCd())) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		UserAttendanceQuery query = UserAttendanceQuery.from(param);

		UserInfoResult user = tbm04Mapper.selectUserInfo(query);
		if (user == null) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		// 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 사용자만
		verifyScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), user.siteCd());

		// 인가 전제: AT.CMPNY_CD = gvCmpnyCd(내 회사 출결행) 가 인가 그 자체다. 세션 헤더 조인만
		// 타사 세션까지 허용해 "자기 직원의 타사 세션 이수 이력"을 볼 수 있게 한다(요청서 §3.4).
		List<UserAttendanceResult> list = tbm04Mapper.selectUserAttendances(query);
		int totalCount = tbm04Mapper.selectUserAttendanceCount(query);
		UserAttendanceSummaryResult summaryResult = tbm04Mapper.selectUserAttendanceSummary(query);

		// F10(N+1 제거): 타사 세션의 개최사 라벨을 행마다 조회하지 않고 세션코드 집합 단위로 1회 배치 조회한다.
		List<String> foreignSessionCds = new ArrayList<>();
		if (list != null) {
			for (UserAttendanceResult r : list) {
				if (r.hostCmpnyCd() != null && !r.hostCmpnyCd().equals(param.gvCmpnyCd())) {
					foreignSessionCds.add(r.sessionCd());
				}
			}
		}
		Map<String, String> hostLabels =
				tbmSessionShareService.resolveHostLabels(foreignSessionCds, param.gvCmpnyCd());

		List<UserAttendanceResponse.AttendanceItem> items = new ArrayList<>();
		if (list != null) {
			for (UserAttendanceResult r : list) {
				boolean foreign = r.hostCmpnyCd() != null && !r.hostCmpnyCd().equals(param.gvCmpnyCd());

				// PRAFTA-SUBCON-T5(plan D3): 타사 세션 행은 사업장명을 내리지 않는다(타사 사업장 =
				// 인접 차수 밖 정보). 대신 "개최 회사"(= 나를 지정한 직상위 회사) 라벨만 표시한다.
				String hostCmpnyNm = foreign ? hostLabels.get(r.sessionCd()) : null;

				items.add(UserAttendanceResponse.AttendanceItem.builder()
						.attendanceCd(r.attendanceCd())
						.sessionCd(r.sessionCd())
						.sessionTitle(r.sessionTitle())
						.siteCd(foreign ? null : r.siteCd())
						.siteNm(foreign ? null : r.siteNm())
						.hostCmpnyNm(hostCmpnyNm)
						.sessionDate(r.sessionDate())
						.entryAt(r.entryAt())
						.exitAt(r.exitAt())
						.completionStatusCd(r.completionStatusCd())
						.completionStatusNm(r.completionStatusNm())
						.riskCount(r.riskCount())
						.build());
			}
		}

		UserAttendanceResponse.Summary summary = buildUserSummary(summaryResult);

		log.info("TBM 사용자별 이수 조회 완료 - userCd={}, userType={}, count={}, totalCount={}",
				param.userCd(), param.userTypeCd(), items.size(), totalCount);

		return UserAttendanceResponse.builder()
				.user(user)
				.attendances(items)
				.totalCount(totalCount)
				.page(param.page())
				.pageSize(param.pageSize())
				.summary(summary)
				.build();
	}

	// ============================ 내부 헬퍼 ============================

	/** W-12 기간 통계 요약 변환(평균 이수율 산출, 빈 데이터 0). */
	private HistorySessionListResponse.StatSummary buildStatSummary(HistoryStatResult stat) {
		if (stat == null) {
			return HistorySessionListResponse.StatSummary.builder()
					.sessionCount(0)
					.attendanceCount(0)
					.completedCount(0)
					.notCompletedCount(0)
					.avgCompletionRate(0d)
					.build();
		}
		double rate = completionRate(stat.completedCount(), stat.attendanceCount());
		return HistorySessionListResponse.StatSummary.builder()
				.sessionCount(stat.sessionCount())
				.attendanceCount(stat.attendanceCount())
				.completedCount(stat.completedCount())
				.notCompletedCount(stat.notCompletedCount())
				.avgCompletionRate(rate)
				.build();
	}

	/** W-15 사용자 통계 요약 변환(이수율 산출, 빈 데이터 0). */
	private UserAttendanceResponse.Summary buildUserSummary(UserAttendanceSummaryResult result) {
		if (result == null) {
			return UserAttendanceResponse.Summary.builder()
					.totalCount(0)
					.completedCount(0)
					.notCompletedCount(0)
					.completionRate(0d)
					.avgDurationMin(null)
					.build();
		}
		double rate = completionRate(result.completedCount(), result.totalCount());
		return UserAttendanceResponse.Summary.builder()
				.totalCount(result.totalCount())
				.completedCount(result.completedCount())
				.notCompletedCount(result.notCompletedCount())
				.completionRate(rate)
				.avgDurationMin(result.avgDurationMin())
				.build();
	}

	/** 이수율(%) = 이수/총참여 * 100. 분모 0 이면 0. 소수 첫째 자리 반올림. */
	private double completionRate(int completed, int total) {
		if (total <= 0) {
			return 0d;
		}
		double rate = (completed * 100d) / total;
		return Math.round(rate * 10d) / 10d;
	}

	/**
	 * 이상신호 레벨 산출(백엔드 권위). 클라이언트 계산 금지.
	 * - HIGH: GPS 범위 이탈 1회 이상, 또는 백그라운드 전환 3회 이상
	 * - LOW: 백그라운드/네트워크 단절 등 경미한 신호 1회 이상
	 * - NONE: 신호 없음
	 */
	private String resolveAnomalyLevel(int backgroundCount, int gpsOutOfRangeCount, int networkLostCount) {
		if (gpsOutOfRangeCount > 0 || backgroundCount >= 3) {
			return ANOMALY_HIGH;
		}
		if (backgroundCount > 0 || networkLostCount > 0) {
			return ANOMALY_LOW;
		}
		return ANOMALY_NONE;
	}

	/** W-14 권한 게이트: 개설자 본인 / safe / master. */
	private void verifyCompletionAuth(String authCd, String ownUserCd, String managerUserCd) {
		if (AuthRoleUtils.canManageCommon(authCd)) {
			return;	// master / safe
		}
		if (ownUserCd != null && ownUserCd.equals(managerUserCd)) {
			return;	// 개설자 본인
		}
		log.warn("TBM 미이수 처리 권한 없음 - authCd={}, ownUserCd={}, managerUserCd={}",
				authCd, ownUserCd, managerUserCd);
		throw new ApiException(TbmErrorCode.TBM_403_020);
	}

	// ============================ W-13 확장: 출결 서명 이미지 ============================

	/**
	 * 출결 서명 이미지 스트림(입실/종료). 2026-08-30 — 출결 상세 팝업 '서명' 버튼이 파일코드 텍스트만
	 * 보여주던 것을 실제 이미지 표시로 교체하기 위한 EP.
	 *
	 * <p>인가: 출결 명단(selectSessionAttendances)과 동일 수준 —
	 * ①권한 미부여 차단 ②세션이 내 회사(개설사) 소유(매퍼 INNER JOIN 강제) ③사업장 스코프 격리.
	 * 파일 식별자는 서버가 출결 행에서 재조회한다(클라 파일코드 신뢰 금지 — IDOR 방지).
	 * 타사 참석자의 서명 파일은 참석자 회사 스코프로 저장되어 있어 attendeeCmpnyCd 로 로드한다.
	 */
	@Override
	public com.prafta.common.cmm.file.application.model.FileBytesResult loadAttendanceSignImage(
			com.prafta.web.tbm.tbm04.application.param.AttendanceSignImageParam param) {

		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 출결 서명 이미지 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}

		com.prafta.web.tbm.tbm04.result.AttendanceSignInfoResult info =
				tbm04Mapper.selectAttendanceSignInfo(param.gvCmpnyCd(), param.attendanceCd());
		if (info == null) {
			// 출결 없음 또는 타사 세션(개설사 아님) — 존재 비노출 통합 404.
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		// 스코프 격리: 회사 전체 권한이 아니면 접근 권한 보유 사업장 세션의 출결만.
		verifyScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), info.sessionSiteCd());

		// [security High #1, 2026-08-30] 관리자 역할 게이트 — 사업장 스코프만으로는 같은 사업장
		// 일반/일용직 근로자('99999')도 도달해 attendanceCd 열거로 자필 서명(PII) 실물을 수집할 수 있다
		// (신규 조회 EP 노드 게이트 누락 3회 재발 패턴). 전사 관리자(master/hr/safe/system) 또는
		// 해당 사업장의 노드(부서) 정/부 관리자만 허용한다(Baim_05 assertSlotWriteRole 패턴 미러 —
		// 노드 관리자인 TBM 개설자의 열람은 막지 않는다).
		if (!AuthRoleUtils.canManageSite(param.gvAuthCd())
				&& tbm04Mapper.countNodeAdminInSite(param.gvCmpnyCd(), info.sessionSiteCd(), param.gvUserCd()) <= 0) {
			log.warn("TBM 출결 서명 이미지 접근 차단(관리자 역할 아님) - userCd={}, authCd={}, siteCd={}",
					param.gvUserCd(), param.gvAuthCd(), info.sessionSiteCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}

		String fileMgmtCd = "ENTRY".equals(param.kind())
				? info.entrySignFileMgmtCd()
				: info.exitSignFileMgmtCd();
		if (!StringUtils.hasText(fileMgmtCd)) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		com.prafta.common.cmm.file.application.model.FileBytesResult file = fileService.loadFileBytes(
				new com.prafta.common.cmm.file.application.query.FileReadQuery(info.attendeeCmpnyCd(), fileMgmtCd));
		if (file == null) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		log.info("TBM 출결 서명 이미지 스트림 - attendanceCd={}, kind={}", param.attendanceCd(), param.kind());
		return file;
	}

	/** 스코프 격리: 회사 전체 권한이 아니면 접근 권한 보유 사업장(User_03 원장 포함) 리소스만 접근 가능. */
	private void verifyScope(String cmpnyCd, String userCd, String authCd, String ownSiteCd, String targetSiteCd) {
		if (AuthRoleUtils.isCompanyWide(authCd)) {
			return;
		}
		if (!siteAccessService.hasSiteAccess(cmpnyCd, userCd, authCd, ownSiteCd, targetSiteCd)) {
			log.warn("TBM 이력 스코프 위반 - authCd={}, ownSite={}, targetSite={}",
					authCd, ownSiteCd, targetSiteCd);
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
	}

	// ============================ TBM 증빙자료 출력(반기, 2026-08-30) ============================

	/** 교육일지(건별) 상세 조회 청크 상한 — 화면이 50건씩 나눠 호출한다(단일 응답 비대화 방지). */
	private static final int EVIDENCE_DETAIL_CHUNK_MAX = 50;

	@Override
	public EvidenceSessionListResponse selectEvidenceSessions(EvidenceListParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 증빙 세션 목록 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}

		EvidenceQuery query = EvidenceQuery.from(param);

		// 자사 개설(사업장 필터 적용) + 공유 세션(자사 참석분 — 사업장 필터 미적용, 권장안) 병합.
		List<com.prafta.web.tbm.tbm04.result.EvidenceSessionResult> merged = new ArrayList<>();
		List<com.prafta.web.tbm.tbm04.result.EvidenceSessionResult> own =
				tbm04Mapper.selectEvidenceOwnSessions(query);
		List<com.prafta.web.tbm.tbm04.result.EvidenceSessionResult> shared =
				tbm04Mapper.selectEvidenceSharedSessions(query);
		if (own != null) merged.addAll(own);
		if (shared != null) merged.addAll(shared);
		// 종료일시 오름차순 통합 정렬(각 쿼리는 이미 정렬 — 병합 후 재정렬).
		merged.sort(java.util.Comparator.comparing(
				r -> r.endedAt() == null ? "" : r.endedAt()));

		log.info("TBM 증빙 세션 목록 조회 - cmpnyCd={}, 기간={}~{}, own={}, shared={}",
				param.gvCmpnyCd(), query.fromDate(), query.toDate(),
				own != null ? own.size() : 0, shared != null ? shared.size() : 0);

		return EvidenceSessionListResponse.builder()
				.sessionList(merged)
				.fromDate(query.fromDate())
				.toDate(query.toDate())
				.cmpnyNm(tbm04Mapper.selectCmpnyNm(param.gvCmpnyCd()))
				.build();
	}

	@Override
	public EvidenceWorkerSummaryResponse selectEvidenceWorkerSummary(EvidenceListParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 증빙 근로자 집계 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}

		EvidenceQuery query = EvidenceQuery.from(param);
		List<com.prafta.web.tbm.tbm04.result.EvidenceWorkerSummaryResult> list =
				tbm04Mapper.selectEvidenceWorkerSummary(query);

		log.info("TBM 증빙 근로자 집계 조회 - cmpnyCd={}, 기간={}~{}, rows={}",
				param.gvCmpnyCd(), query.fromDate(), query.toDate(), list != null ? list.size() : 0);

		return EvidenceWorkerSummaryResponse.builder()
				.workerList(list != null ? list : Collections.emptyList())
				.build();
	}

	@Override
	public EvidenceSessionDetailResponse selectEvidenceSessionDetails(List<String> sessionCds,
			String gvCmpnyCd, String gvSiteCd, String gvAuthCd) {

		if (AuthRoleUtils.isAccessDenied(gvAuthCd)) {
			log.warn("TBM 증빙 교육일지 접근 차단 - authCd={}", gvAuthCd);
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
		if (sessionCds == null || sessionCds.isEmpty()) {
			throw new ApiException(com.prafta.common.error.common.CommonErrorCode.COMMON_400_001);
		}
		if (sessionCds.size() > EVIDENCE_DETAIL_CHUNK_MAX) {
			throw new ApiException(com.prafta.common.error.common.CommonErrorCode.COMMON_400_002);
		}

		boolean companyWide = AuthRoleUtils.isCompanyWide(gvAuthCd);

		// 1) 세션 개요 — 인가(자사 개설(스코프) OR 자사 참석 존재)는 쿼리 술어가 강제, 미인가는 조용히 제외.
		List<com.prafta.web.tbm.tbm04.result.EvidenceSessionDetailResult> sessions =
				tbm04Mapper.selectEvidenceSessionDetails(gvCmpnyCd, companyWide, gvSiteCd, sessionCds);
		if (sessions == null || sessions.isEmpty()) {
			return EvidenceSessionDetailResponse.builder()
					.sessionList(Collections.emptyList())
					.attendeeList(Collections.emptyList())
					.riskList(Collections.emptyList())
					.mtrlList(Collections.emptyList())
					.build();
		}

		// 2) 부속 목록은 인가 통과분 sessionCd 로만 조회(요청 원본 목록 사용 금지 — IDOR 차단).
		List<String> allowedCds = sessions.stream()
				.map(com.prafta.web.tbm.tbm04.result.EvidenceSessionDetailResult::sessionCd)
				.toList();

		log.info("TBM 증빙 교육일지 상세 조회 - cmpnyCd={}, 요청={}건, 인가통과={}건",
				gvCmpnyCd, sessionCds.size(), allowedCds.size());

		return EvidenceSessionDetailResponse.builder()
				.sessionList(sessions)
				.attendeeList(tbm04Mapper.selectEvidenceAttendees(gvCmpnyCd, allowedCds))
				.riskList(tbm04Mapper.selectEvidenceRisks(allowedCds))
				.mtrlList(tbm04Mapper.selectEvidenceMtrls(allowedCds))
				.build();
	}
}
