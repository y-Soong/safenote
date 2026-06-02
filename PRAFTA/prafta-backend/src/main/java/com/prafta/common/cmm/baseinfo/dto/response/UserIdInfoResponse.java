package com.prafta.common.cmm.baseinfo.dto.response;

import com.prafta.common.cmm.baseinfo.result.UserIdInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserIdInfoResponse {
	private UserIdInfoResult userIdInfoResult;
}
