package com.prafta.web.chkLst.chkLst03.result;

public record InspectAnswerResult(
	String cmpnyCd						// 회사코드
	, String chkptCd						// 점검항목코드
	, String inspectItemCd				// 점검문항코드
	, String workDate
	, String inspectAnswerType
	, String answerDesc
	, String fileMgmtCd
	, String filePath
) {
	
}
