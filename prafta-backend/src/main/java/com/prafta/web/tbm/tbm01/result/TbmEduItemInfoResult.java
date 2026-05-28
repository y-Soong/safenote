package com.prafta.web.tbm.tbm01.result;

public record TbmEduItemInfoResult(
	String chk
	, String mtrlItemCd
	, String mtrlCd
	, String sortIdx
	, String mtrlItemType
	, String mtrlDesc
	, String fileMgmtCd
	, String fileNm
	, String filePath
	, String fileExt
	, String url
	, String useYn

	, String thumbFileMgmtCd	// prafta-033-A: 썸네일 파일코드
	, String durationSec		// prafta-033-A: 미디어 길이(초, 동영상)

	/* 데이터 초기화용 값 */
	, String oriSortIdx
	, String oriMtrlItemType
	, String oriMtrlDesc
	, String oriFileMgmtCd
	, String oriFileNm
	, String oriFilePath
	, String oriFileExt
	, String oriUrl
	, String oriUseYn
){

}
