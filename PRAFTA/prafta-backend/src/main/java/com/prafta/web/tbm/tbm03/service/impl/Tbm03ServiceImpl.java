package com.prafta.web.tbm.tbm03.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.tbm.tbm03.application.param.UserProgressDetailParam;
import com.prafta.web.tbm.tbm03.application.param.UserProgressListParam;
import com.prafta.web.tbm.tbm03.application.query.UserProgressDetailQuery;
import com.prafta.web.tbm.tbm03.application.query.UserProgressListQuery;
import com.prafta.web.tbm.tbm03.dto.response.UserProgressDetailResponse;
import com.prafta.web.tbm.tbm03.dto.response.UserProgressListResponse;
import com.prafta.web.tbm.tbm03.mapper.Tbm03Mapper;
import com.prafta.web.tbm.tbm03.result.UserProgressDetailResult;
import com.prafta.web.tbm.tbm03.result.UserProgressListResult;
import com.prafta.web.tbm.tbm03.result.UserProgressSummaryResult;
import com.prafta.web.tbm.tbm03.result.UserProgressUserResult;
import com.prafta.web.tbm.tbm03.service.Tbm03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 진행관리(Tbm_03). PRAFTA-WEB_002-T7.
 *
 * <p>사용자별 TBM 누적 교육시간/수료·미이수/최근 이수일 집계 + 세션 이수 이력 드릴다운. 순수 조회.
 * 정규직(TB_USER)/일용직(TB_DAILY_USER) 통합. 사업장 스코프(공통 §8.3~8.5)는 tbm04 패턴 동일.
 *
 * <p>집계 정준정의(메모리 app-031, tbm04 동일):
 * <ul>
 *   <li>세션: DEL_YN='N' AND STATUS_CD != 'CANCELLED'</li>
 *   <li>출결: DEL_YN='N'. 모수 = 입실(ENTRY_AT IS NOT NULL)</li>
 *   <li>이수(수료) = COMPLETION_STATUS_CD = 'COMPLETED'</li>
 *   <li>미이수 = 입실했으나 (COMPLETION_STATUS_CD IS NULL OR = 'NOT_COMPLETED')</li>
 *   <li>미입실(ENTRY_AT IS NULL) = 집계 제외</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Tbm03ServiceImpl implements Tbm03Service {

	private final Tbm03Mapper tbm03Mapper;

	// ============================ T7 집계 목록 ============================

	@Override
	public UserProgressListResponse selectUserProgressList(UserProgressListParam param) {
		// 999999(권한 미부여)는 화면 진입 차단
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 진행관리 목록 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}

		UserProgressListQuery query = UserProgressListQuery.from(param);

		List<UserProgressListResult> list = tbm03Mapper.selectUserProgressList(query);
		int totalCount = tbm03Mapper.selectUserProgressListCount(query);

		log.info("TBM 진행관리 목록 조회 완료 - cmpnyCd={}, count={}, totalCount={}",
				param.gvCmpnyCd(), list != null ? list.size() : 0, totalCount);

		return UserProgressListResponse.builder()
				.progressList(list != null ? list : Collections.emptyList())
				.totalCount(totalCount)
				.page(param.page())
				.pageSize(param.pageSize())
				.build();
	}

	// ============================ T7 드릴다운 ============================

	@Override
	public UserProgressDetailResponse selectUserProgressDetail(UserProgressDetailParam param) {
		if (AuthRoleUtils.isAccessDenied(param.gvAuthCd())) {
			log.warn("TBM 진행관리 드릴다운 접근 차단 - authCd={}", param.gvAuthCd());
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
		if (!StringUtils.hasText(param.userCd())) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		UserProgressDetailQuery query = UserProgressDetailQuery.from(param);

		// 대상 사용자 헤더 조회(없으면 404)
		UserProgressUserResult user = tbm03Mapper.selectUserProgressUser(query);
		if (user == null) {
			throw new ApiException(TbmErrorCode.TBM_404_020);
		}

		// 스코프 격리: 회사 전체 권한이 아니면 대상 사용자 사업장이 자기 사업장과 일치해야 함(tbm04 흐름 복제)
		verifyScope(param.gvAuthCd(), param.gvSiteCd(), user.siteCd());

		List<UserProgressDetailResult> list = tbm03Mapper.selectUserProgressDetail(query);
		int totalCount = tbm03Mapper.selectUserProgressDetailCount(query);
		UserProgressSummaryResult summaryResult = tbm03Mapper.selectUserProgressSummary(query);

		UserProgressDetailResponse.Summary summary = buildSummary(summaryResult);

		log.info("TBM 진행관리 드릴다운 조회 완료 - userCd={}, userType={}, count={}, totalCount={}",
				param.userCd(), param.userTypeCd(), list != null ? list.size() : 0, totalCount);

		return UserProgressDetailResponse.builder()
				.user(user)
				.summary(summary)
				.attendances(list != null ? list : Collections.emptyList())
				.totalCount(totalCount)
				.page(param.page())
				.pageSize(param.pageSize())
				.build();
	}

	// ============================ 내부 헬퍼 ============================

	/** 드릴다운 요약 변환(빈 데이터 0 보정). */
	private UserProgressDetailResponse.Summary buildSummary(UserProgressSummaryResult result) {
		if (result == null) {
			return UserProgressDetailResponse.Summary.builder()
					.totalEduMinutes(0)
					.completedCount(0)
					.notCompletedCount(0)
					.build();
		}
		return UserProgressDetailResponse.Summary.builder()
				.totalEduMinutes(result.totalEduMinutes())
				.completedCount(result.completedCount())
				.notCompletedCount(result.notCompletedCount())
				.build();
	}

	/** 스코프 격리: 회사 전체 권한이 아니면 자기 사업장 리소스만 접근 가능. */
	private void verifyScope(String authCd, String ownSiteCd, String targetSiteCd) {
		if (AuthRoleUtils.isCompanyWide(authCd)) {
			return;
		}
		if (ownSiteCd == null || !ownSiteCd.equals(targetSiteCd)) {
			log.warn("TBM 진행관리 스코프 위반 - authCd={}, ownSite={}, targetSite={}",
					authCd, ownSiteCd, targetSiteCd);
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
	}
}
