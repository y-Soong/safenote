package com.prafta.web.chkLst.chkLst03.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst03.result.InspectAnswerResult;
import com.prafta.web.chkLst.chkLst03.result.InspectItemHistResult;
import com.prafta.web.chkLst.chkLst03.result.InspectItemSubjResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InspectResultDetailResponse {
	List<InspectItemSubjResult> inspectItemSubjResultList;
	List<InspectAnswerResult> inspectAnswerResultList;
	List<InspectItemHistResult> inspectItemHistResultList;	// 문항 변경이력(확인서 회색 게이팅용)
}
