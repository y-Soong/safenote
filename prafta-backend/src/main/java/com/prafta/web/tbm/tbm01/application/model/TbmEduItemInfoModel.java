package com.prafta.web.tbm.tbm01.application.model;

public record TbmEduItemInfoModel(
	String mtrlItemCd
	, String mtrlCd
	, String mtrlItemType
	, String sortIdx
	, String fileMgmtCd
	, String mtrlDesc
	, String url
	, String useYn

	, String thumbFileMgmtCd	// prafta-033-A: 썸네일 파일코드
	, String durationSec		// prafta-033-A: 미디어 길이(초, 동영상)

	/** Optional: new file as Base64 when using JSON save (not persisted). */
	, String itemBase64
	, String itemOriginalFilename
){

}
