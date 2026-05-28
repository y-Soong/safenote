package com.prafta.web.tbm.tbm04.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.tbm.tbm04.application.command.CompletionUpdateCommand;
import com.prafta.web.tbm.tbm04.application.query.AttendanceEventQuery;
import com.prafta.web.tbm.tbm04.application.query.AttendanceGuardQuery;
import com.prafta.web.tbm.tbm04.application.query.HistorySessionListQuery;
import com.prafta.web.tbm.tbm04.application.query.SessionAttendanceQuery;
import com.prafta.web.tbm.tbm04.application.query.UserAttendanceQuery;
import com.prafta.web.tbm.tbm04.result.AttendanceEventResult;
import com.prafta.web.tbm.tbm04.result.AttendanceGuardResult;
import com.prafta.web.tbm.tbm04.result.HistorySessionListResult;
import com.prafta.web.tbm.tbm04.result.HistoryStatResult;
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
}
