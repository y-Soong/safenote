package com.prafta.app.chkLst.chkLst01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChecklistInfoSave {
	String cmpnyCd;
	String siteCd;
	String inspectItemCd;
	String workDate;
	String inspectAnswerType;
	String answerDesc;
	String fileMgmtCd;
}
