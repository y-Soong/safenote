package com.prafta.web.chkLst.chkLst03.result;

/**
 * 순회점검 문항 변경이력(TB_CHKPT_INSPECT_ITEM_HIST) 조회 결과.
 * 확인서(ChkLstRstPop) 셀 회색 게이팅을 이력 기반으로 판정하기 위한 상태 스냅샷.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 함.
 */
public record InspectItemHistResult(
	String inspectItemCd		// 점검항목코드
	, String chgType			// 변경유형(01:등록, 02:수정, 03:사용중지, 04:재사용)
	, String useYn				// 변경 후 사용유무(Y/N)
	, String strDate			// 변경 후 시행일(YYYYMMDD)
	, String chgYmd				// 변경일(YYYYMMDD)
) {

}
