package com.prafta.web.tbm.tbm04.result;

/**
 * W-13 출결 명단 행. 정규직/일용직 통합(USER_TYPE_CD 분기 조인).
 *
 * <p>일용직은 평문 휴대폰 금지 — MBL_NO_LAST4(끝 4자리)만 노출. 정규직은 소속(부서)명 노출.
 * 이상신호 요약(background/duration/gps/network/anomaly)은 TB_TBM_ATTENDANCE_EVENT 백엔드 집계.
 */
public record SessionAttendanceResult(
	String attendanceCd
	, String userTypeCd			// REGULAR / DAILY
	, String userTypeNm
	, String userCd
	, String userNm
	, String deptNm				// 정규직 소속 부서명(일용직은 NULL)
	, String mblNoLast4			// 일용직 끝 4자리(정규직은 NULL)
	, String entryTypeCd
	, String entryAt
	, String exitTypeCd
	, String exitAt				// NULL/빈칸 = 미종료
	, String exitForcedReason	// 강제종료 사유
	, String entrySignFileMgmtCd
	, String exitSignFileMgmtCd
	, String completionStatusCd	// COMPLETED / NOT_COMPLETED / NULL
	, String completionStatusNm
	, String notCompletedReason
	, String statusUpdatedBy
	, String statusUpdatedByNm
	, String statusUpdatedAt
	, Integer appForegroundSec	// prafta-051-08: 앱 포그라운드 누적초(SELF_DEVICE만, NULL=미수신/대리입실)
	, Integer entryDistanceM	// prafta-051-16: 입실 GPS 거리(m), 대리/검색입실 NULL
	// ===== 이상신호 요약(백엔드 집계) =====
	, int backgroundCount		// BACKGROUND_OUT 발생 횟수
	, int gpsOutOfRangeCount	// GPS 범위 이탈 횟수
	, int networkLostCount		// NETWORK_LOST 발생 횟수
	, int eventCount			// 전체 이벤트 수
	// PRAFTA-SUBCON-T5: 참석자 소속 회사코드(서비스가 1차 relabel 하여 응답에 담는다. 코드 자체는 비노출)
	, String cmpnyCd
){
}
