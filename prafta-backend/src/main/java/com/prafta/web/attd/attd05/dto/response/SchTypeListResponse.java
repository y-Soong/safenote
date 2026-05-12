package com.prafta.web.attd.attd05.dto.response;

import java.util.List;

import com.prafta.web.attd.attd05.result.SchTypeResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchTypeListResponse {
	List<SchTypeResult> schTypeResultList;
}
