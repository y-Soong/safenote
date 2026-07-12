package com.prafta.web.tbm.tbm03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.tbm.tbm03.application.query.UserProgressDetailQuery;
import com.prafta.web.tbm.tbm03.application.query.UserProgressListQuery;
import com.prafta.web.tbm.tbm03.result.UserProgressDetailResult;
import com.prafta.web.tbm.tbm03.result.UserProgressListResult;
import com.prafta.web.tbm.tbm03.result.UserProgressSummaryResult;
import com.prafta.web.tbm.tbm03.result.UserProgressUserResult;

@Mapper
public interface Tbm03Mapper {

	/* ===== T7 사용자별 진행 집계 목록 ===== */
	/** 사용자별 집계 목록(단일 GROUP BY, 행당 추가 쿼리 없음). */
	List<UserProgressListResult> selectUserProgressList(UserProgressListQuery query);

	/** 집계 목록 건수(동일 WHERE GROUP BY 서브쿼리 COUNT). */
	int selectUserProgressListCount(UserProgressListQuery query);

	/* ===== T7 드릴다운 ===== */
	/** 드릴다운 대상 사용자 헤더(정규직/일용직 유형별 분기, 스코프 검증용 siteCd 포함). */
	UserProgressUserResult selectUserProgressUser(UserProgressDetailQuery query);

	/** 드릴다운 세션 이수 이력 행(페이징). */
	List<UserProgressDetailResult> selectUserProgressDetail(UserProgressDetailQuery query);

	int selectUserProgressDetailCount(UserProgressDetailQuery query);

	/** 드릴다운 요약 통계(누적 교육시간/수료/미이수). */
	UserProgressSummaryResult selectUserProgressSummary(UserProgressDetailQuery query);
}
