package com.prafta.web.tbm.tbm02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
import com.prafta.web.tbm.tbm02.application.model.SessionRiskModel;
import com.prafta.web.tbm.tbm02.application.query.OptionQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionDetailQuery;
import com.prafta.web.tbm.tbm02.application.query.SessionListQuery;
import com.prafta.web.tbm.tbm02.application.query.SharedSessionListQuery;
import com.prafta.web.tbm.tbm02.result.SharedSessionResult;
import com.prafta.web.tbm.tbm02.application.query.EntryCandidateQuery;
import com.prafta.web.tbm.tbm02.application.query.EntryTargetQuery;
import com.prafta.web.tbm.tbm02.result.AutoStartTargetResult;
import com.prafta.web.tbm.tbm02.result.ContentOptionResult;
import com.prafta.web.tbm.tbm02.result.RiskOptionResult;
import com.prafta.web.tbm.tbm02.result.SessionContentResult;
import com.prafta.web.tbm.tbm02.result.SessionGuardResult;
import com.prafta.web.tbm.tbm02.result.SessionListResult;
import com.prafta.web.tbm.tbm02.result.SessionResult;
import com.prafta.web.tbm.tbm02.result.SessionRiskResult;
import com.prafta.web.tbm.tbm02.result.SiteOptionResult;
import com.prafta.web.tbm.tbm02.result.AttendanceSlotResult;
import com.prafta.web.tbm.tbm02.result.EntryCandidateResult;
import com.prafta.web.tbm.tbm02.result.SessionAttendanceResult;

@Mapper
public interface Tbm02Mapper {

	/* ===== 채번 ===== */
	/** 세션코드 채번 (T + YYYYMMDD + SEQ). */
	String selectSessionCd(@Param("gvCmpnyCd") String gvCmpnyCd);

	/* ===== W-04 목록 ===== */
	List<SessionListResult> selectSessionList(SessionListQuery query);

	int selectSessionListCount(SessionListQuery query);

	/* ===== PRAFTA-SUBCON-T5 D2: 연동받은 교육(비개설사 전용) ===== */
	/** 내 회사가 유효하게 지정받은 타사 세션 목록(헤더 최소 필드). 스코프 SQL 자체가 인가다. */
	List<SharedSessionResult> selectSharedSessionList(SharedSessionListQuery query);

	int selectSharedSessionListCount(SharedSessionListQuery query);

	/* ===== W-06 상세 ===== */
	SessionResult selectSessionDetail(SessionDetailQuery query);

	List<SessionContentResult> selectSessionContents(SessionDetailQuery query);

	List<SessionRiskResult> selectSessionRisks(SessionDetailQuery query);

	/** 상태/스코프 게이트 검증용 경량 조회. */
	SessionGuardResult selectSessionGuard(SessionDetailQuery query);

	/* ===== W-05/06 쓰기 ===== */
	void insertSession(SessionCommand command);

	void updateSession(SessionCommand command);

	void insertSessionContent(SessionContentCommand command);

	void deleteSessionContents(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("sessionCd") String sessionCd);

	void insertSessionRisk(SessionRiskCommand command);

	/** 세션-위험성평가 연계 검증: 제출 키(siteCd/processCd/assessmentCd)가 TB_RISK_ASSESSMENT 에 실존하는 건수(중복 제거 키 전제). */
	int countRiskAssessments(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("keys") List<SessionRiskModel> keys);

	void deleteSessionRisks(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("sessionCd") String sessionCd);

	void upsertSessionState(SessionStateCommand command);

	void cancelSession(SessionCancelCommand command);

	/* ===== PRAFTA-051 상태머신 재설계 ===== */

	/** 교육준비(OPENED) 전이: 입실비번 발급 + GPS 중심좌표 + PREP_START_AT 확정. WHERE STATUS_CD='DRAFT' 경합 가드. */
	int prepareSession(SessionPrepareCommand command);

	/** 입실비번만 재발급(OPENED). 종료비번 미변경. */
	int updateEntryPwd(SessionSinglePwdCommand command);

	/** 종료비번만 재발급(COMPLETED). 입실비번 미변경. */
	int updateExitPwd(SessionSinglePwdCommand command);

	/** 교육시작(IN_PROGRESS) 전이: STARTED_AT=NOW(). WHERE STATUS_CD='OPENED' 경합 가드. */
	int startSession(SessionStateTransitionCommand command);

	/** 교육준비 연장: PREP_START_AT=NOW() 리셋. WHERE STATUS_CD='OPENED' AND 15분 미경과 경합 가드. */
	int extendPrep(SessionStateTransitionCommand command);

	/** 교육종료(COMPLETED) 전이: ENDED_AT=NOW() + 종료비번 발급. WHERE STATUS_CD='IN_PROGRESS' 경합 가드. */
	int completeSession(SessionStateTransitionCommand command);

	/** 15분 자동 교육시작 배치(prafta-051-06): OPENED AND PREP_START_AT+15분 경과 → IN_PROGRESS 일괄 전이. */
	int bulkStartExpiredPrep();

	/**
	 * PRAFTA-APP-021-3b(W3 자동시작): 자동전이 대상(OPENED + PREP_START_AT 15분 경과) 세션 키 목록.
	 * bulkStartExpiredPrep 와 동일 WHERE 로, 일괄 전이 직전 시작 PUSH 대상(cmpnyCd/siteCd/managerUserCd)을 포착한다.
	 */
	List<AutoStartTargetResult> selectExpiredPrepForStart();

	/* ===== PRAFTA-051-11 관리자 대리/검색 입실 ===== */

	/** 정규직 입실 후보 검색(세션 사업장 활성 사용자 + alreadyEntered). */
	List<EntryCandidateResult> selectRegularCandidates(EntryCandidateQuery query);

	/** 일용직 입실 후보 검색(C7 만료/탈퇴 필터 + alreadyEntered). */
	List<EntryCandidateResult> selectDailyCandidates(EntryCandidateQuery query);

	/**
	 * 대리입실 대상 유효성 + 소속 회사 도출(PRAFTA-SUBCON-T5 F2).
	 * 대상 사용자가 실제로 속한 회사코드를 체인 범위(targetCmpnyCds) 안에서 반환한다.
	 * 빈 목록=대상 부적합. 2건 이상=회사간 USER_CD 충돌(회사별 채번) → 서비스가 거부한다.
	 */
	List<String> selectEntryTargetCmpnyCds(EntryTargetQuery query);

	/** 대리입실 슬롯(UNIQUE 키) 점유 행 조회. 없으면 null(→INSERT), 있으면 DEL_YN 으로 분기. */
	AttendanceSlotResult selectAttendanceSlot(ManagerEnterCommand command);

	/** 관리자 직접 입실 INSERT(MANAGER_DIRECT, GPS/거리/비번 NULL). */
	void insertManagerEntry(ManagerEnterCommand command);

	/** 내보내기 후 재입실: DEL_YN='Y' 행을 'N' 으로 복구 + MANAGER_DIRECT 재기록. */
	int restoreManagerEntry(ManagerEnterCommand command);

	/* ===== PRAFTA-051-12 입실자 명단/내보내기 ===== */

	/** 교육준비 단계 입실자 명단(DEL_YN='N', 정규직/일용직 통합, 거리/입실유형 포함). */
	List<SessionAttendanceResult> selectSessionAttendances(SessionDetailQuery query);

	/** 입실자 내보내기(soft delete): DEL_YN='Y' + EXIT_FORCED_REASON. 세션 일치 + DEL_YN='N' 만. */
	int ejectAttendance(EjectAttendanceCommand command);

	/* ===== 보조 조회 ===== */
	List<ContentOptionResult> selectContentOptions(OptionQuery query);

	List<RiskOptionResult> selectRiskOptions(OptionQuery query);

	List<SiteOptionResult> selectSiteOptions(@Param("gvCmpnyCd") String gvCmpnyCd);
}
