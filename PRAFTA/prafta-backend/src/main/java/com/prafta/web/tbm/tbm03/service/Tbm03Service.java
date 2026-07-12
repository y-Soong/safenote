package com.prafta.web.tbm.tbm03.service;

import com.prafta.web.tbm.tbm03.application.param.UserProgressDetailParam;
import com.prafta.web.tbm.tbm03.application.param.UserProgressListParam;
import com.prafta.web.tbm.tbm03.dto.response.UserProgressDetailResponse;
import com.prafta.web.tbm.tbm03.dto.response.UserProgressListResponse;

public interface Tbm03Service {

	/** T7 사용자별 TBM 진행 집계 목록(정규+일용 통합, 사업장 스코프). */
	UserProgressListResponse selectUserProgressList(UserProgressListParam param);

	/** T7 사용자별 세션 이수 이력 드릴다운(헤더 + 요약 + 세션 목록). */
	UserProgressDetailResponse selectUserProgressDetail(UserProgressDetailParam param);
}
