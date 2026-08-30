package com.prafta.web.tbm.tbm04.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.tbm.tbm04.application.command.CompletionUpdateCommand;
import com.prafta.web.tbm.tbm04.application.query.AttendanceEventQuery;
import com.prafta.web.tbm.tbm04.application.query.AttendanceGuardQuery;
import com.prafta.web.tbm.tbm04.application.query.EvidenceQuery;
import com.prafta.web.tbm.tbm04.application.query.HistorySessionListQuery;
import com.prafta.web.tbm.tbm04.application.query.SessionAttendanceQuery;
import com.prafta.web.tbm.tbm04.application.query.UserAttendanceQuery;
import com.prafta.web.tbm.tbm04.result.AttendanceEventResult;
import com.prafta.web.tbm.tbm04.result.AttendanceGuardResult;
import com.prafta.web.tbm.tbm04.result.AttendanceSignInfoResult;
import com.prafta.web.tbm.tbm04.result.EvidenceAttendeeResult;
import com.prafta.web.tbm.tbm04.result.EvidenceMtrlResult;
import com.prafta.web.tbm.tbm04.result.EvidenceRiskResult;
import com.prafta.web.tbm.tbm04.result.EvidenceSessionDetailResult;
import com.prafta.web.tbm.tbm04.result.EvidenceSessionResult;
import com.prafta.web.tbm.tbm04.result.EvidenceWorkerSummaryResult;
import com.prafta.web.tbm.tbm04.result.HistorySessionListResult;
import com.prafta.web.tbm.tbm04.result.HistoryStatResult;
import com.prafta.web.tbm.tbm04.result.ManagerSignInfoResult;
import com.prafta.web.tbm.tbm04.result.SessionAttendanceResult;
import com.prafta.web.tbm.tbm04.result.UserAttendanceResult;
import com.prafta.web.tbm.tbm04.result.UserAttendanceSummaryResult;
import com.prafta.web.tbm.tbm04.result.UserInfoResult;

@Mapper
public interface Tbm04Mapper {

	/* ===== W-12 이력 목록 ===== */
	List<HistorySessionListResult> selectHistorySessionList(HistorySessionListQuery query);

	int selectHistorySessionListCount(HistorySessionListQuery query);

	/** 기간 통계 요약(목록 필터와 동일 스코프). */
	HistoryStatResult selectHistoryStat(HistorySessionListQuery query);

	/* ===== W-13 출결 명단 / 이벤트 ===== */
	/** 출결 명단(정규직/일용직 유형별 분기 조인, 이상신호 요약 집계 포함). */
	List<SessionAttendanceResult> selectSessionAttendances(SessionAttendanceQuery query);

	/** 이벤트 타임라인(시간순, 페이징). */
	List<AttendanceEventResult> selectAttendanceEvents(AttendanceEventQuery query);

	int selectAttendanceEventCount(AttendanceEventQuery query);

	/* ===== W-13 확장: 출결 서명 이미지 ===== */
	/** 서명 파일 식별 정보(세션이 내 회사 소유일 때만 행 반환 — 클라 파일코드 신뢰 금지). */
	AttendanceSignInfoResult selectAttendanceSignInfo(@org.apache.ibatis.annotations.Param("gvCmpnyCd") String gvCmpnyCd, @org.apache.ibatis.annotations.Param("attendanceCd") String attendanceCd);

	/** 해당 사업장의 노드(부서) 정/부 관리자 여부(서명 이미지 역할 게이트 — Baim05 동형). 1 이상이면 관리자. */
	int countNodeAdminInSite(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd, @org.apache.ibatis.annotations.Param("siteCd") String siteCd, @org.apache.ibatis.annotations.Param("userCd") String userCd);

	/* ===== tbm04-manager-sign: 주관자 서명 이미지 ===== */
	/** 주관자 서명 파일 식별 정보 + 인가 판단 재료(ownerYn/attendedYn). 세션 없으면 null. */
	ManagerSignInfoResult selectManagerSignInfo(
			@org.apache.ibatis.annotations.Param("gvCmpnyCd") String gvCmpnyCd,
			@org.apache.ibatis.annotations.Param("sessionCd") String sessionCd);

	/* ===== W-14 미이수 처리 ===== */
	/** 게이트 검증용: 출결 + 소속 세션 메타(스코프/개설자). */
	AttendanceGuardResult selectAttendanceGuard(AttendanceGuardQuery query);

	/** 이수상태 사후 변경 UPDATE(STATUS_UPDATED_BY/AT 기록). */
	int updateCompletion(CompletionUpdateCommand command);

	/* ===== W-15 사용자별 이수 ===== */
	/** 대상 사용자 헤더 정보(정규직/일용직 유형별 분기 조인). */
	UserInfoResult selectUserInfo(UserAttendanceQuery query);

	/** 사용자별 이수 이력 행. */
	List<UserAttendanceResult> selectUserAttendances(UserAttendanceQuery query);

	int selectUserAttendanceCount(UserAttendanceQuery query);

	/** 사용자별 이수 통계(이수/미이수/평균 참여시간). */
	UserAttendanceSummaryResult selectUserAttendanceSummary(UserAttendanceQuery query);

	/* ===== TBM 증빙자료 출력(반기, 2026-08-30) ===== */
	/** 자사 개설 종료 세션(반기·스코프·사업장 필터). */
	List<EvidenceSessionResult> selectEvidenceOwnSessions(EvidenceQuery query);

	/** 타사 개설 공유 세션 중 자사 근로자 참석분(반기 — 사업장 필터 미적용, 카운트=자사 기준). */
	List<EvidenceSessionResult> selectEvidenceSharedSessions(EvidenceQuery query);

	/** 근로자별 반기 이수 집계(자사 활성 정규직 전원 + 이수 기록 있는 자사 일용직). */
	List<EvidenceWorkerSummaryResult> selectEvidenceWorkerSummary(EvidenceQuery query);

	/** 교육일지(건별) 세션 개요 — 인가(자사 개설(스코프) OR 자사 참석 존재) 통과분만 반환. */
	List<EvidenceSessionDetailResult> selectEvidenceSessionDetails(
			@org.apache.ibatis.annotations.Param("gvCmpnyCd") String gvCmpnyCd,
			@org.apache.ibatis.annotations.Param("companyWide") boolean companyWide,
			@org.apache.ibatis.annotations.Param("scopeSiteCd") String scopeSiteCd,
			@org.apache.ibatis.annotations.Param("sessionCds") List<String> sessionCds);

	/** 교육일지(건별) 참석자 — 자사 세션=전원(타사 소속 표기)/공유 세션=자사 참석자만(쿼리 술어 강제). */
	List<EvidenceAttendeeResult> selectEvidenceAttendees(
			@org.apache.ibatis.annotations.Param("gvCmpnyCd") String gvCmpnyCd,
			@org.apache.ibatis.annotations.Param("sessionCds") List<String> sessionCds);

	/** 교육일지(건별) 위험성평가 연계 목록. */
	List<EvidenceRiskResult> selectEvidenceRisks(
			@org.apache.ibatis.annotations.Param("sessionCds") List<String> sessionCds);

	/** 교육일지(건별) 교육자료 목록. */
	List<EvidenceMtrlResult> selectEvidenceMtrls(
			@org.apache.ibatis.annotations.Param("sessionCds") List<String> sessionCds);

	/** 회사명(TB_CMPNY) — 증빙 엑셀 머리말 표기용. 없으면 null. */
	String selectCmpnyNm(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd);
}
