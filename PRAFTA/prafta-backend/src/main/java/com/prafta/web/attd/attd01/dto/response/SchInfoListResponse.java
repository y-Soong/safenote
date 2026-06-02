package com.prafta.web.attd.attd01.dto.response;

import java.util.List;

import com.prafta.web.attd.attd01.result.SchInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchInfoListResponse{
	private List<SchInfoResult> schInfoResultList;
}
