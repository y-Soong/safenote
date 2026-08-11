package com.prafta.web.attd.attd05.result;

/**
 * 근무타입(SCH_CD) effective-dating 검증용 버전 행.
 * TB_SCH_MGMT(현재본) + TB_SCH_MGMT_HIST(이력본) 합집합의 단일 버전 한 건을 표현한다.
 */
public record SchTypeUseYnResult(
	String schCd
	, String applyDate
	, String useYn
	/**
	 * 해당 버전의 근무타입 표시 라벨(시각 + 고정연장 병기 — selectSchTypeList 의 schNm 표기 규칙 동일).
	 * FE 적용 미리보기가 날짜별 유효 버전의 시각을 표시하는 데 사용한다(Attd05 시점별 시각 표시).
	 * (record 위치 매핑 — SELECT 마지막 컬럼과 순서 일치, 말미 추가 유지)
	 */
	, String schNm
) {

}
