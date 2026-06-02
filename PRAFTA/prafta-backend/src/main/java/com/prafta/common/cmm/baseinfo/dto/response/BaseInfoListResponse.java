package com.prafta.common.cmm.baseinfo.dto.response;

import java.util.List;

import com.prafta.common.cmm.baseinfo.result.BaseInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BaseInfoListResponse {
	List<BaseInfoResult> baseInfoList;
}
