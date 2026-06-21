package com.prafta.web.attd.attd05.result;

public record LeaveTypeResult(
	String cmpnyCd
	, String leaveCd
	, String leaveNo
	, String leaveNm
	, String leaveType        // [SYS021] 01 사용자 신청 / 02 관리자 부여 — 신청 화면 필터용
	, String aprvUseYn        // 결재 사용 여부 (Y면 신청 시 결재라인 필요)
	, String leaveNatureType  // [SYS024] 01 법정 / 02 특별 — 근무계획 적용 시 법정만 노출
	// ── prafta-com-011-6 가불(미래 연차 당겨쓰기) 메타 (앱 selectApplyMeta 미러, additive) ──
	, String systemYn         // [SYSTEM_YN] Y 면 시스템 법정 시드(월차/본연차 등) — 가불 대상 판정/신청 노출용
	, java.math.BigDecimal balanceDays // 현재 잔여(LEAVE_TYPE='01'=회계연도 한도−사용분 / 그외=GRANT−USED). 가불 토글 노출 판정용
	, Boolean borrowable      // 가불 가능 여부(시스템 법정 월차/본연차 + 한도>0). 서비스에서 산정
	, java.math.BigDecimal borrowQuota // 가불 가능 한도(일). 비대상이면 0. 서비스에서 산정
	, String borrowExpiryYmd  // 가불분 만료(소멸)일 YYYYMMDD. 산정 불가/비대상이면 null
) {

}

