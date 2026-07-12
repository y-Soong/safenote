package com.prafta.web.tbm.tbm03.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm03.result.UserProgressListResult;

import lombok.Builder;
import lombok.Getter;

/** T7 사용자별 진행 집계 목록 응답(목록 + 페이징). */
@Getter
@Builder
public class UserProgressListResponse {
	private List<UserProgressListResult> progressList;
	private int totalCount;
	private int page;
	private int pageSize;
}
