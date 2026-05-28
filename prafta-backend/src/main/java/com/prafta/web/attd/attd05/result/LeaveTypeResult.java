package com.prafta.web.attd.attd05.result;

public record LeaveTypeResult(
	String cmpnyCd
	, String leaveCd
	, String leaveNo
	, String leaveNm
	, String leaveType        // [SYS021] 01 사용자 신청 / 02 관리자 부여 — 신청 화면 필터용
	, String aprvUseYn        // 결재 사용 여부 (Y면 신청 시 결재라인 필요)
	, String leaveNatureType  // [SYS024] 01 법정 / 02 특별 — 근무계획 적용 시 법정만 노출
) {

}

