package com.prafta.common.cmm.baseinfo.dto.response;

import com.prafta.common.cmm.baseinfo.result.CmpnyInfoResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CmpnyInfoResponse {
	CmpnyInfoResult cmpnyInfoResult;
}
