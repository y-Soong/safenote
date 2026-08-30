package com.prafta.web.tbm.tbm04.service;

import java.util.List;

import com.prafta.web.tbm.tbm04.application.param.AttendanceEventParam;
import com.prafta.web.tbm.tbm04.application.param.CompletionUpdateParam;
import com.prafta.web.tbm.tbm04.application.param.EvidenceListParam;
import com.prafta.web.tbm.tbm04.application.param.HistorySessionListParam;
import com.prafta.web.tbm.tbm04.application.param.SessionAttendanceParam;
import com.prafta.web.tbm.tbm04.application.param.UserAttendanceParam;
import com.prafta.web.tbm.tbm04.dto.response.AttendanceEventResponse;
import com.prafta.web.tbm.tbm04.dto.response.EvidenceSessionDetailResponse;
import com.prafta.web.tbm.tbm04.dto.response.EvidenceSessionListResponse;
import com.prafta.web.tbm.tbm04.dto.response.EvidenceWorkerSummaryResponse;
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

	/** W-13 확장 — 출결 서명 이미지 스트림(입실/종료). 파일코드는 서버 재조회(IDOR 방지). */
	com.prafta.common.cmm.file.application.model.FileBytesResult loadAttendanceSignImage(
			com.prafta.web.tbm.tbm04.application.param.AttendanceSignImageParam param);

	/** tbm04-manager-sign — 주관자 서명 이미지 스트림. 파일코드는 서버 재조회(IDOR 방지). */
	com.prafta.common.cmm.file.application.model.FileBytesResult loadManagerSignImage(
			com.prafta.web.tbm.tbm04.application.param.ManagerSignImageParam param);

	/** W-14 미이수 처리(이수/미이수 사후 변경). */
	void updateCompletion(CompletionUpdateParam param);

	/** W-15 사용자별 이수 이력(정규직/일용직). */
	UserAttendanceResponse selectUserAttendances(UserAttendanceParam param);

	/* ===== TBM 증빙자료 출력(반기, 2026-08-30) ===== */

	/** 반기 세션 목록(자사 개설 + 공유 세션 자사 참석분). */
	EvidenceSessionListResponse selectEvidenceSessions(EvidenceListParam param);

	/** 근로자별 반기 이수 집계(인정시간 축). */
	EvidenceWorkerSummaryResponse selectEvidenceWorkerSummary(EvidenceListParam param);

	/** 교육일지(건별) 상세 — 청크 최대 50건, 미인가 세션은 조용히 제외. */
	EvidenceSessionDetailResponse selectEvidenceSessionDetails(List<String> sessionCds,
			String gvCmpnyCd, String gvSiteCd, String gvAuthCd);
}
