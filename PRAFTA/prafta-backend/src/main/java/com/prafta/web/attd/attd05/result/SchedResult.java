package com.prafta.web.attd.attd05.result;

public record SchedResult(
	String cmpnyCd
	, String siteCd
	, String userCd
	, String workYmd
	, String workPlanCd
	/**
	 * 근무일(WORK_YMD) 기준 유효 버전으로 해석한 근무타입 표시 라벨(시각 + 고정연장 병기).
	 * 근무타입 시점 변경(effective-dating) 시 과거 날짜 셀이 현재본 시각으로 표시되던 결함 수정.
	 * 근무타입이 아닌 코드(레거시 휴가코드 등)이거나 버전 미존재면 null — FE 가 기존 경로로 폴백.
	 * (record 위치 매핑 — SELECT 마지막 컬럼과 순서 일치, 말미 추가 유지)
	 */
	, String schNm
) {

}

