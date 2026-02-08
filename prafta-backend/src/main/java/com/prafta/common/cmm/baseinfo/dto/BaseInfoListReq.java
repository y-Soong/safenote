package com.prafta.common.cmm.baseinfo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseInfoListReq {
	List<String> baseCodeList;
	String cmpnyCd;
}
