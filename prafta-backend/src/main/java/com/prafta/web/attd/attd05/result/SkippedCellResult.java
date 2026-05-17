package com.prafta.web.attd.attd05.result;

/**
 * 근무타입 검증 위반으로 저장에서 스킵된 셀 정보.
 * 프론트가 사용자에게 스킵 사유를 표시하는 데 사용한다.
 * - reasonCode : BEFORE_CREATE(생성 전) / USE_YN_N(미사용 기간)
 */
public record SkippedCellResult(
	String userCd
	, String workYmd
	, String workPlanCd
	, String reasonCode
	, String reason
) {

}
