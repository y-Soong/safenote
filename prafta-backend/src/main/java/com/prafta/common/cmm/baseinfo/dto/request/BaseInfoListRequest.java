package com.prafta.common.cmm.baseinfo.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseInfoListRequest {
	private String cmpnyCd;
	private List<String> baseCodeList;
}
