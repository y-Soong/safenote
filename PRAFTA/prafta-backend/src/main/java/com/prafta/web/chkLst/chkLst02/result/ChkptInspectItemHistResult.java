package com.prafta.web.chkLst.chkLst02.result;

/**
 * 순회점검 문항 변경이력 조회 결과(문항관리 이력 팝업용).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 함.
 */
public record ChkptInspectItemHistResult(
	String chgType				// 변경유형(01:등록, 02:수정, 03:사용중지, 04:재사용)
	, String chgTypeNm			// 변경유형명
	, String inspectItemSubj	// 변경 후 점검항목명칭
	, String strDate			// 변경 후 시행일(YYYYMMDD)
	, Integer sortIdx			// 변경 후 정렬순서
	, String useYn				// 변경 후 사용유무(Y/N)
	, String chgUserNm			// 변경자명
	, String chgDtime			// 변경일시(YYYY-MM-DD HH:mm)
){

}
