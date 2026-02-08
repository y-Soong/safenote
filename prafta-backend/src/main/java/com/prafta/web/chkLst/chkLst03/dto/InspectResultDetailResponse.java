package com.prafta.web.chkLst.chkLst03.dto;

import java.util.List;

import com.prafta.web.chkLst.chkLst03.vo.InspectAnswer;
import com.prafta.web.chkLst.chkLst03.vo.InspectItemSubj;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class InspectResultDetailResponse {
	List<InspectItemSubj> inspectItemSubjList;
	List<InspectAnswer> inspectAnswerList;
}
