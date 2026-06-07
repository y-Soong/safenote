package com.prafta.web.chkLst.chkLst04.result;

public record DefectResult(
	String cmpnyCd							// 회사코드
	, String siteCd							// 사업장코드
	, String siteNm							// 사업장명
	, String chkLstType						// 점검구분[COM001]
	, String chkLstTypeNm					// 점검구분명
	, String chkptCd						// 점검대상코드
	, String chkptNm						// 점검대상명칭
	, String inspectItemCd					// 점검항목코드
	, String inspectItemSubj				// 점검항목명
	, String inspectorNm					// 점검자명(INSERT_NO -> USER_NM)
	, String workDate						// 점검일자(YYYYMMDD)
	, String answerDesc						// 불량 비고(ANSWER_DESC) — 상세 팝업 동봉(#3)
	, String fileMgmtCd						// 첨부사진 코드(+확장자) — 상세 팝업 동봉(#3)
	, String filePath						// 첨부사진 경로 — 상세 팝업 동봉(#3)
	, String actionDesc						// 조치 상세 내역(없으면 null = 미조치)
	, String actionStatus					// 조치여부 파생: 'Y' 조치완료 / 'N' 미조치
) {

}
