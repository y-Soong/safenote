package com.prafta.web.attd.attd05.result;

/**
 * 근무타입(SCH_CD) effective-dating 검증용 버전 행.
 * TB_SCH_MGMT(현재본) + TB_SCH_MGMT_HIST(이력본) 합집합의 단일 버전 한 건을 표현한다.
 */
public record SchTypeUseYnResult(
	String schCd
	, String applyDate
	, String useYn
) {

}
