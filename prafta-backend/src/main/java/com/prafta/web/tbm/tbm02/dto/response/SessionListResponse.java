package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.SessionListResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionListResponse {
	private List<SessionListResult> sessionList;
	private int totalCount;
	private int page;
	private int pageSize;
}
