package com.prafta.web.chkLst.chkLst04.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst04.result.DefectResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefectListResponse {
	private List<DefectResult> defectResultList;
}
