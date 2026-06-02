package com.prafta.web.chkLst.chkLst02.application.model;

public record ChkptInspectItemModel(
	String cmpnyCd
	, String siteCd
	, String chkLstType
	, String inspectItemCd
	, String inspectItemSubj
	, int sortIdx
	, String strDate
	, String useYn
	, String gvCmpnyCd
	, String gvUserCd
) {
	
}
