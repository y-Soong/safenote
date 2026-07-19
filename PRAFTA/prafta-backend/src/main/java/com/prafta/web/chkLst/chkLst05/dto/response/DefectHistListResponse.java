package com.prafta.web.chkLst.chkLst05.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst05.result.DefectHistResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefectHistListResponse {
	private List<DefectHistResult> defectHistList;
}
