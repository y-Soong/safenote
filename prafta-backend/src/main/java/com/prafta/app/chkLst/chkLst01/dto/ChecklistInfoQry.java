package com.prafta.app.chkLst.chkLst01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChecklistInfoQry {
	String cmpnyCd;
	String siteCd;
	String chkptCd;
	String chkptNm;
}
