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

	/** Optional: new file as Base64 when using JSON save (not persisted). */
	, String itemBase64
	, String itemOriginalFilename
){

}
