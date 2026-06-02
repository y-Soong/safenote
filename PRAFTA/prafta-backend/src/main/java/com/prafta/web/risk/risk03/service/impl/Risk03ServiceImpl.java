package com.prafta.web.risk.risk03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.application.query.FileInfoQuery;
import com.prafta.common.cmm.file.dto.param.FileInfoParam;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
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
	}

}
