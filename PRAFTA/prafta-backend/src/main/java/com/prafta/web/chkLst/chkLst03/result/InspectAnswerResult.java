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
	// PRAFTA-SUBCON-T6-07: 수행 주체(점검자) 표시 — 전파 행의 INSERT_NO 는 'SYSTEM' 이라 사용자 표시에 쓸 수 없다.
	//   performCmpnyCd/Nm 은 그 테넌트에서 보이는 인접 1차 회사(relabel 저장값), performUserNm 은 저장 시점 스냅샷.
	//   record 위치 매핑이라 신규 필드는 SELECT 말미와 같은 순서로 말미에 추가한다.
	, String performCmpnyCd
	, String performCmpnyNm
	, String performUserCd
	, String performUserNm
) {

}
