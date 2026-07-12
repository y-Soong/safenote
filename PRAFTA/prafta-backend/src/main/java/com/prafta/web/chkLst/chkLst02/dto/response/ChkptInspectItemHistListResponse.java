package com.prafta.web.chkLst.chkLst02.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemHistResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChkptInspectItemHistListResponse {
	List<ChkptInspectItemHistResult> chkptInspectItemHistResultList;
}
