package com.prafta.web.tbm.tbm01.application.model;

public record TbmEduMtrlModel(
	String mtrlCd
	, String title
	, String contents
	, String mtrlType
	, String useYn
	, String siteCd			// prafta-033-A: 스코프(NULL/빈값=회사공통)
	, String gvCmpnyCd
	, String gvUserCd
){

}
