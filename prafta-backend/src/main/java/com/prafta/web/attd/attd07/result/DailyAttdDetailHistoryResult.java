package com.prafta.web.attd.attd07.result;

public record DailyAttdDetailHistoryResult(
	String cmpnyCd
	, String histId
	, String attdId
	, String histType
	, String histTypeNm
	, String workSeq
	
	, String befCheckInDate
	, String befCheckInTime
	, String befCheckOutDate
	, String befCheckOutTime

	, String aftCheckInDate
	, String aftCheckInTime
	, String aftCheckOutDate
	, String aftCheckOutTime

	, String processReason
	, String insertNo
	, String insertNm
	, String insertDate
) {
}
