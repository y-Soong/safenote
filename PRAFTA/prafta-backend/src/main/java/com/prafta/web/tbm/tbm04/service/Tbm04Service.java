package com.prafta.web.tbm.tbm04.service;

import com.prafta.web.tbm.tbm04.application.param.AttendanceEventParam;
import com.prafta.web.tbm.tbm04.application.param.CompletionUpdateParam;
import com.prafta.web.tbm.tbm04.application.param.HistorySessionListParam;
import com.prafta.web.tbm.tbm04.application.param.SessionAttendanceParam;
import com.prafta.web.tbm.tbm04.application.param.UserAttendanceParam;
import com.prafta.web.tbm.tbm04.dto.response.AttendanceEventResponse;
import com.prafta.web.tbm.tbm04.dto.response.HistorySessionListResponse;
import com.prafta.web.tbm.tbm04.dto.response.SessionAttendanceResponse;
import com.prafta.web.tbm.tbm04.dto.response.UserAttendanceResponse;

public interface Tbm04Service {

	/** W-12 이력 목록(COMPLETED/CANCELLED 위주 + 기간 통계). */
	HistorySessionListResponse selectHistorySessionList(HistorySessionListParam param);

	/** W-13 세션 출결 명단(유형별 분기 조인, 이상신호 요약). */
	SessionAttendanceResponse selectSessionAttendances(SessionAttendanceParam param);

	/** W-13 출결 단건 이벤트 타임라인(시간순). */
	AttendanceEventResponse selectAttendanceEvents(AttendanceEventParam param);

	/** W-14 미이수 처리(이수/미이수 사후 변경). */
	void updateCompletion(CompletionUpdateParam param);

	/** W-15 사용자별 이수 이력(정규직/일용직). */
	UserAttendanceResponse selectUserAttendances(UserAttendanceParam param);
}
