package com.prafta.web.attd.attd05.application.model;

public record SchTypeModel(
	String siteCd
	, String userCd
	, String workYmd
	, String workPlanCd
	// prafta-com-016-C-2: 연차 셀 적용 시 휴가 종류(SYS_ANNUAL|SYS_MONTHLY). SCH 셀이면 null.
	, String leaveCd
	// prafta-com-016-C-4: 종류 미지정 "법정 휴가" 자동 적용 셀 여부(소멸 임박 통합순 자동 차감).
	, boolean autoLegalLeave
	, String gvCmpnyCd
	, String gvUserCd
) {

}
