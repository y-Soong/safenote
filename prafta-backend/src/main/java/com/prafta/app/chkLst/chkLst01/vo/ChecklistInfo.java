package com.prafta.app.chkLst.chkLst01.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class ChecklistInfo {
	String cmpnyCd;
	String chklstType;
	String chkptDesc;
	String inspectItemCd;
	String inspectItemSubj;
	String inspectValue;
	String sortIdx;
}
