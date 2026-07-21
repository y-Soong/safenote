package com.prafta.web.tbm.tbm03.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.tbmshare.service.TbmSessionShareService;
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
	/** PRAFTA-SUBCON-T5 F4: 타사(연동) 세션의 개최 회사 라벨 해석(하향 인접 차수 가시성). */
	private final TbmSessionShareService tbmSessionShareService;
	/** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
	private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

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
		verifyScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), user.siteCd());

		// 인가 전제: 매퍼의 AT.CMPNY_CD = gvCmpnyCd(내 직원 출결행)가 인가 그 자체다.
		// F4: 세션 조인에서 CMPNY_CD 를 뺐으므로 타사(연동) 세션 참석 이력도 포함된다(요청서 §3.4).
		List<UserProgressDetailResult> list = tbm03Mapper.selectUserProgressDetail(query);
		int totalCount = tbm03Mapper.selectUserProgressDetailCount(query);
		UserProgressSummaryResult summaryResult = tbm03Mapper.selectUserProgressSummary(query);

		// 타사 세션의 개최 회사 라벨 배치 조회(N+1 회피). 자사 세션은 라벨 없음.
		List<String> foreignSessionCds = new ArrayList<>();
		if (list != null) {
			for (UserProgressDetailResult r : list) {
				if (r.hostCmpnyCd() != null && !r.hostCmpnyCd().equals(param.gvCmpnyCd())) {
					foreignSessionCds.add(r.sessionCd());
				}
			}
		}
		Map<String, String> hostLabels =
				tbmSessionShareService.resolveHostLabels(foreignSessionCds, param.gvCmpnyCd());

		List<UserProgressDetailResponse.AttendanceItem> items = new ArrayList<>();
		if (list != null) {
			for (UserProgressDetailResult r : list) {
				boolean foreign = r.hostCmpnyCd() != null && !r.hostCmpnyCd().equals(param.gvCmpnyCd());

				items.add(UserProgressDetailResponse.AttendanceItem.builder()
						.attendanceCd(r.attendanceCd())
						.sessionCd(r.sessionCd())
						.sessionTitle(r.sessionTitle())
						.sessionDate(r.sessionDate())
						.eduMinutes(r.eduMinutes())
						.entryAt(r.entryAt())
						.exitAt(r.exitAt())
						.completionStatusCd(r.completionStatusCd())
						.completionStatusNm(r.completionStatusNm())
						// D3: 타사 세션은 개최 회사만 표시(타사 사업장/회사코드 비노출).
						.hostCmpnyNm(foreign ? hostLabels.get(r.sessionCd()) : null)
						.build());
			}
		}

		UserProgressDetailResponse.Summary summary = buildSummary(summaryResult);

		log.info("TBM 진행관리 드릴다운 조회 완료 - userCd={}, userType={}, count={}, totalCount={}",
				param.userCd(), param.userTypeCd(), items.size(), totalCount);

		return UserProgressDetailResponse.builder()
				.user(user)
				.summary(summary)
				.attendances(items)
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

	/** 스코프 격리: 회사 전체 권한이 아니면 접근 권한 보유 사업장(User_03 원장 포함) 리소스만 접근 가능. */
	private void verifyScope(String cmpnyCd, String userCd, String authCd, String ownSiteCd, String targetSiteCd) {
		if (AuthRoleUtils.isCompanyWide(authCd)) {
			return;
		}
		if (!siteAccessService.hasSiteAccess(cmpnyCd, userCd, authCd, ownSiteCd, targetSiteCd)) {
			log.warn("TBM 진행관리 스코프 위반 - authCd={}, ownSite={}, targetSite={}",
					authCd, ownSiteCd, targetSiteCd);
			throw new ApiException(TbmErrorCode.TBM_403_021);
		}
	}
}
