package com.prafta.web.baim.baim03.dto;

import java.util.List;

import com.prafta.web.baim.baim03.vo.TermsInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TermsInfoListRes{
	List<TermsInfo> termsInfoList;
}
