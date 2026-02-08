package com.prafta.common.cmm.baseinfo.dto;

import java.util.List;

import com.prafta.common.cmm.baseinfo.vo.BaseInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BaseInfoListRes {
	List<BaseInfo> baseInfoList;
}
