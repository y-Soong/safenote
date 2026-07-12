package com.prafta.web.risk.risk03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.cmm.push.RiskAssessNotiConst;
import com.prafta.common.cmm.push.RiskAssessNotiService;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.risk.RiskErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.risk03.application.command.AssessmentCommand;
import com.prafta.web.risk.risk03.application.param.AssessmentParam;
import com.prafta.web.risk.risk03.application.param.RiskAssessmentsListParam;
import com.prafta.web.risk.risk03.application.param.RiskTypeInfoListParam;
import com.prafta.web.risk.risk03.application.query.RiskAssessmentsListQuery;
import com.prafta.web.risk.risk03.application.query.RiskTypeInfoListQuery;
import com.prafta.web.risk.risk03.dto.response.RiskAssessmentsListResponse;
import com.prafta.web.risk.risk03.dto.response.RiskTypeListResponse;
import com.prafta.web.risk.risk03.mapper.Risk03Mapper;
import com.prafta.web.risk.risk03.result.RiskAssessmentResult;
import com.prafta.web.risk.risk03.result.RiskTypeResult;
import com.prafta.web.risk.risk03.service.Risk03Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Risk03ServiceImpl implements Risk03Service{
	
	private final Risk03Mapper risk03Mapper;
	private final FileService fileService;
    private final FileMapper fileMapper;
    /** PRAFTA-APP-021-3d(M5): 위험성평가 검토요청 통보 PUSH 생산자(safe/노드 관리자, afterCommit 격리). */
    private final RiskAssessNotiService riskAssessNotiService;
	
	public RiskTypeListResponse selectRiskTypeInfoList(RiskTypeInfoListParam param) {
		
		RiskTypeListResponse response = null;
		
		List<RiskTypeResult> riskTypeResultList = risk03Mapper.selectRiskTypeList(RiskTypeInfoListQuery.from(param));
		
		if(riskTypeResultList != null &&riskTypeResultList.size() > 0) {
			response = RiskTypeListResponse.builder()
					.riskTypeResultList(riskTypeResultList)
					.build();
		}
		
		return response;		
	}
	
	public RiskAssessmentsListResponse selectRiskAssessmentsLists(RiskAssessmentsListParam param) {
		
		RiskAssessmentsListResponse response = null;
		
		List<RiskAssessmentResult> riskAssessmentResultList = risk03Mapper.selectRiskAssessmentsLists(RiskAssessmentsListQuery.from(param));
		
		if(riskAssessmentResultList.size() > 0) {
			response = RiskAssessmentsListResponse.builder()
											.riskAssessmentResultList(riskAssessmentResultList)
											.build();
		}
		
		return response;
	}
	
	@Transactional
	public void saveAssessment(AssessmentParam param, MultipartFile file) {
		// PRAFTA_COM_001_T6 Low-1A: 위험도는 클라 전송값(revalRiskLv 등)을 신뢰하지 않고 서버에서 빈도 x 강도로 재계산한다(단일 출처).
		//   재계산 결과를 003 가드와 저장(INIT/REVAL_RISK_LV) 양쪽에 동일하게 사용하여 위조에 의한 게이트 우회를 차단한다.
		int initRiskLvCalc = calcRiskLv(param.initLikelihoodScore(), param.initSeverityScore());		// 0 = 계산 불가
		int revalRiskLvCalc = calcRiskLv(param.revalLikelihoodScore(), param.revalSeverityScore());	// 0 = 계산 불가

		// T6-14B-1: 개선완료(003) 저장은 "개선 후" 위험도가 "매우낮음"(1~3)일 때만 허용 (fail-closed).
		//   점수 누락/비정상으로 재계산 불가(0)이면 차단한다. 001/검토요청 등 003 외 전이는 본 가드를 거치지 않으므로 무회귀.
		if ("003".equals(param.assessmentStatus())) {
			if (revalRiskLvCalc < 1 || revalRiskLvCalc > 3) {
				throw new ApiException(RiskErrorCode.RISK_400_002);
			}
		}

		// 저장용 위험도 문자열(재계산값). 계산 불가(0)이면 기존 동작과 동일하게 "0"으로 저장한다.
		String initRiskLvToSave = String.valueOf(initRiskLvCalc);
		String revalRiskLvToSave = String.valueOf(revalRiskLvCalc);

		try {
			String fileMgmtCd = "";
    		if (file != null && !file.isEmpty()) {

    			fileMgmtCd = fileMapper.selectFileMgmtCd(FileInfoQuery.from(param.gvCmpnyCd(), "002"));			// 002 : 위험성평가

    			fileService.fileSave(FileInfoParam.from(
    					param.gvCmpnyCd()
    					, param.gvUserCd()
    					, param.siteCd()
    					, "002"							// 위험성 평가
    					, fileMgmtCd
    					, file
				));
    		}

    		risk03Mapper.updateAssessment(AssessmentCommand.from(param, fileMgmtCd, initRiskLvToSave, revalRiskLvToSave));

		} catch (ApiException e) {
			// 가드/비즈니스 예외는 원본 코드/메시지를 보존하기 위해 그대로 재전파(500 둔갑 방지)
			throw e;
		} catch (Exception e) {
			log.error("위험성평가 저장 실패. assessmentCd={}", param.assessmentCd(), e);
			throw new ApiException(CommonErrorCode.COMMON_500_001);
		}

		// PRAFTA-APP-021-3d(M5): "검토 요청"(001) 전이 시 safe/노드 관리자에게 통보 PUSH 적재(afterCommit 격리).
		//   저장 본 흐름이 커밋된 뒤에만 적재되며, 적재 실패는 저장에 영향을 주지 않는다.
		if (RiskAssessNotiConst.STATUS_REVIEW_REQUESTED.equals(param.assessmentStatus())) {
			try {
				riskAssessNotiService.notifyReviewRequested(
						param.gvCmpnyCd(), param.siteCd(), param.assessmentCd(), param.gvUserCd(), param.gvUserCd());
			} catch (Exception e) {
				log.error("위험성평가 검토요청 통보 PUSH 적재 hook 실패(저장 영향 없음). assessmentCd={}", param.assessmentCd(), e);
			}
		}
	}

	/**
	 * 위험도 서버 재계산: 빈도 x 강도. 빈도(1~5)·강도(1~4)가 모두 정상 범위일 때만 곱을 반환하고,
	 * 어느 한쪽이라도 null/공백/비숫자/범위 밖이면 계산 불가로 보아 0을 반환한다(가드는 fail-closed).
	 */
	private int calcRiskLv(String likelihoodScore, String severityScore) {
		int likelihood = parseScore(likelihoodScore);
		int severity = parseScore(severityScore);
		// Low-A: 빈도 1~5, 강도 1~4 범위 밖이면 비정상 곱 저장 방지(계산 불가=0)
		if (likelihood < 1 || likelihood > 5 || severity < 1 || severity > 4) {
			return 0;
		}
		return likelihood * severity;
	}

	/** 점수 문자열 안전 파싱. null/공백/비숫자/음수는 0(=계산 불가). */
	private int parseScore(String score) {
		if (score == null) {
			return 0;
		}
		try {
			int v = Integer.parseInt(score.trim());
			return v < 0 ? 0 : v;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
