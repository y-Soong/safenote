package com.prafta.web.tbm.tbm04.result;

/** W-13 이벤트 타임라인 행(시간순). EVENT_DATA 는 JSON 문자열 그대로 전달. */
public record AttendanceEventResult(
	long eventNo
	, String eventTypeCd
	, String eventTypeNm
	, String eventTime			// 클라이언트 보고 시각(ms)
	, String serverReceivedAt	// 서버 수신 시각(위조불가 기준)
	, String eventData			// JSON 문자열
){
}
