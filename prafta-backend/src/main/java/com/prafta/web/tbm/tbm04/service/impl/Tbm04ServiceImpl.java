package com.prafta.web.tbm.tbm04.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm04.application.command.CompletionUpdateCommand;
import com.prafta.web.tbm.tbm04.application.param.AttendanceEventParam;
import com.prafta.web.tbm.tbm04.application.param.CompletionUpdateParam;
import com.prafta.web.tbm.tbm04.application.param.HistorySessionListParam;
import com.prafta.web.tbm.tbm04.application.param.SessionAttendanceParam;
import com.prafta.web.tbm.tbm04.application.param.UserAttendanceParam;
import com.prafta.web.tbm.tbm04.application.query.AttendanceEventQuery;
import com.prafta.web.tbm.tbm04.application.query.AttendanceGuardQuery;
import com.prafta.web.tbm.tbm04.application.query.HistorySessionListQuery;
import com.prafta.web.tbm.tbm04.application.query.SessionAttendanceQuery;
import com.prafta.web.tbm.tbm04.application.query.UserAttendanceQuery;
import com.prafta.web.tbm.tbm04.dto.response.AttendanceEventResponse;
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

		SessionAttendanceQuery query = SessionAttendanceQuery.from(param);

		List<SessionAttendanceResult> list = tbm04Mapper.selectSessionAttendances(query);

		List<SessionAttendanceResponse.AttendanceItem> items = new ArrayList<>();
		int completedCount = 0;
		int notCompletedCount = 0;

		if (list != null) {
			for (SessionAttendanceResult r : list) {
				boolean exited = StringUtils.hasText(r.exitAt());
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
						.userCd(r.userCd())
						.userNm(r.userNm())
						.deptNm(r.deptNm())
						.mblNoLast4(r.mblNoLast4())
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
						.backgroundCount(r.backgroundCount())
						.gpsOutOfRangeCount(r.gpsOutOfRangeCount())
						.networkLostCount(r.networkLostCount())
						.eventCount(r.eventCount())
						.anomalyLevel(anomalyLevel)
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
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), guard.siteCd());

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
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), user.siteCd());

		List<UserAttendanceResult> list = tbm04Mapper.selectUserAttendances(query);
		int totalCount = tbm04Mapper.selectUserAttendanceCount(query);
		UserAttendanceSummaryResult summaryResult = tbm04Mapper.selectUserAttendanceSummary(query);

		UserAttendanceResponse.Summary summary = buildUserSummary(summaryResult);

		log.info("TBM 사용자별 이수 조회 완료 - userCd={}, userType={}, count={}, totalCount={}",
				param.userCd(), param.userTypeCd(), list != null ? list.size() : 0, totalCount);

		return UserAttendanceResponse.builder()
				.user(user)
				.attendances(list != null ? list : Collections.emptyList())
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

	/** 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 리소스만 접근 가능. */
	private void verifyScope(String authCd, String ownSiteCd, String targetSiteCd) {
		if (AuthRoleUtils.isCompanyWide(authCd)) {
			return;
		}
		if (ownSiteCd == null || !ownSiteCd.equals(targetSiteCd)) {
			log.warn("TBM 이력 스코프 위반 - authCd={}, ownSite={}, targetSite={}",
					authCd, ownSiteCd, targetSiteCd);
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
	}
}
