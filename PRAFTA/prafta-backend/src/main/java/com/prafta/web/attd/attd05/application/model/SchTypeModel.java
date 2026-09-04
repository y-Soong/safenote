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
	/** BW-04: 요청에 실려 온 휴게시간 무시 값(정상 요청은 null). 'Y' 면 서비스가 ATTD_400_218 거부. */
	, String brkWaiveYn
) {

}
