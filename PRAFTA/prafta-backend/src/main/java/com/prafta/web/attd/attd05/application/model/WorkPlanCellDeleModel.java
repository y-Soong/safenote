package com.prafta.web.attd.attd05.application.model;

/**
 * PRAFTA-041 - 근무계획 셀(사용자+근무일) 단위 삭제 모델.
 */
public record WorkPlanCellDeleModel(
	String siteCd
	, String userCd
	, String workYmd
	, String gvCmpnyCd
	, String gvUserCd
) {

}
