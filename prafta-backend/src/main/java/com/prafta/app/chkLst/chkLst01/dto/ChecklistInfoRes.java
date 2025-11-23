package com.prafta.app.chkLst.chkLst01.dto;

import java.util.List;

import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChecklistInfoRes {
	List<ChecklistInfo> checklistInfos;
}
