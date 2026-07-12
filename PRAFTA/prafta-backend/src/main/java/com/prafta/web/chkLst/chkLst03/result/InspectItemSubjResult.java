package com.prafta.web.chkLst.chkLst03.result;

public record InspectItemSubjResult(
	String InspectItemCd			// 점검문항코드
	, String InspectItemSubj		// 점검문항명
	, String strDate				// 시행일(YYYYMMDD) - PRAFTA_COM_001-T5-11.1.1 (시행월→시행일 전환)
	, String useYn					// 사용여부(Y/N)
	, String insertYmd				// 등록일(YYYYMMDD)
	, String updateYmd				// 최종수정일(YYYYMMDD): 변경이력 부재 시 비활성 시점 폴백
) {

}
