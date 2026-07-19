package com.prafta.web.chkLst.chkLst02.result;

/**
 * 순회점검 문항 조회 결과.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 함 (siteCd 는 cmpnyCd 바로 뒤 — PRAFTA-SUBCON-T0-02).
 */
public record ChkptInspectItemResult(
	String chk
	, String cmpnyCd
	, String siteCd
	, String chkLstType
	, String inspectItemCd
	, String inspectItemSubj
	, int sortIdx
	, String strDate
	, String useYn
	// PRAFTA-SUBCON-T6-03: 연동 원본 회사코드(NULL=자체 문항, NOT NULL=미러 → 화면 '연동' 배지 + 읽기전용, 저장 시 403).
	//   record 위치 매핑이라 신규 필드는 SELECT 말미와 같은 순서로 말미에 추가한다.
	, String linkSrcCmpnyCd
){

}
