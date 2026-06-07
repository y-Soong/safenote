package com.prafta.web.chkLst.chkLst04.result;

public record InspectItemResult(
	String inspectItemCd					// 점검항목코드
	, String inspectItemSubj				// 점검항목명
	, String strDate						// 시행월(YYYYMM)
) {

}
