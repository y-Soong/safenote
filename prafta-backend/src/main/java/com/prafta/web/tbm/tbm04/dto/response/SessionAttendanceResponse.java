package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * W-13 출결 명단 응답. 행마다 이상신호 요약(anomalyLevel 포함, 백엔드 산출)과
 * 이수/입퇴실 정보를 담는다. 빈 세션은 attendanceList 빈 배열.
 */
@Getter
@Builder
public class SessionAttendanceResponse {
	private List<AttendanceItem> attendanceList;
	private int totalCount;
	private int completedCount;
	private int notCompletedCount;

	@Getter
	@Builder
	public static class AttendanceItem {
		private String attendanceCd;
		private String userTypeCd;
		private String userTypeNm;
		private String userCd;
		private String userNm;
		private String deptNm;			// 정규직 소속(일용직 NULL)
		private String mblNoLast4;		// 일용직 끝4자리(정규직 NULL)
		private String entryTypeCd;
		private String entryAt;
		private String exitTypeCd;
		private String exitAt;			// 빈칸=미종료
		private boolean exited;			// 종료 여부(편의 플래그)
		private String exitForcedReason;
		private boolean forcedEnd;		// 강제종료 여부
		private String entrySignFileMgmtCd;
		private String exitSignFileMgmtCd;
		private String completionStatusCd;
		private String completionStatusNm;
		private String notCompletedReason;
		private String statusUpdatedBy;
		private String statusUpdatedByNm;
		private String statusUpdatedAt;
		// ===== 이상신호 요약 =====
		private int backgroundCount;
		private int gpsOutOfRangeCount;
		private int networkLostCount;
		private int eventCount;
		private String anomalyLevel;	// NONE / LOW / HIGH (백엔드 산출)
	}
}
