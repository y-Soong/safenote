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

    		risk03Mapper.updateAssessment(AssessmentCommand.from(param, fileMgmtCd));

		} catch (Exception e) {
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

}
