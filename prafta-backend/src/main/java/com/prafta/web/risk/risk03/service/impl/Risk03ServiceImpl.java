package com.prafta.web.risk.risk03.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.file.dto.FileInfoSave;
import com.prafta.common.cmm.file.mapper.FileMapper;
import com.prafta.common.cmm.file.service.FileService;
import com.prafta.common.exception.RiskApiException;
import com.prafta.web.risk.risk03.dto.RiskAssessmentsListQry;
import com.prafta.web.risk.risk03.dto.RiskAssessmentsListReq;
import com.prafta.web.risk.risk03.dto.RiskAssessmentsListRes;
import com.prafta.web.risk.risk03.dto.RiskTypeListRes;
import com.prafta.web.risk.risk03.dto.SaveAssessmentQry;
import com.prafta.web.risk.risk03.dto.SaveAssessmentReq;
import com.prafta.web.risk.risk03.mapper.Risk03Mapper;
import com.prafta.web.risk.risk03.service.Risk03Service;
import com.prafta.web.risk.risk03.vo.RiskAssessment;
import com.prafta.web.risk.risk03.vo.RiskType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Risk03ServiceImpl implements Risk03Service{
	
	private final Risk03Mapper risk03Mapper;
	private final FileService fileService;
    private final FileMapper fileMapper;
	
	public RiskTypeListRes selectRiskTypeInfoList(Map<String, Object> tokenInfo) {
		
		RiskTypeListRes retDto = null;
		
		List<RiskType> riskTypeList = risk03Mapper.selectRiskTypeList(tokenInfo);
		
		if(riskTypeList.size() > 0) {
			retDto = RiskTypeListRes.builder()
					.riskTypeList(riskTypeList)
					.build();
		}
		
		return retDto;		
	}
	
	public RiskAssessmentsListRes selectRiskAssessmentsLists(RiskAssessmentsListReq dto, Map<String, Object> tokenInfo) {
		
		RiskAssessmentsListQry reqDto = RiskAssessmentsListQry.builder()
				.siteCd(dto.getSiteCd())
				.assessmentStatus(dto.getAssessmentStatus())
				.processCd(dto.getProcessCd())
				.riskTypeCd(dto.getRiskTypeCd())
				.build();
		
		RiskAssessmentsListRes retDto = null;
		
		List<RiskAssessment> riskAssessmentList = risk03Mapper.selectRiskAssessmentsLists(reqDto, tokenInfo);
		
		if(riskAssessmentList.size() > 0) {
			retDto = RiskAssessmentsListRes.builder()
					.riskAssessmentList(riskAssessmentList)
					.build();
		}
		
		return retDto;	
	}
	
	@Transactional
	public void saveAssessment(SaveAssessmentReq request, MultipartFile file, Map<String, Object> tokenInfo) {
		try {
			String fileMgmtCd = "";
    		if (file != null && !file.isEmpty()) {
    			
    			FileInfoSave baseQuery = FileInfoSave.builder()
                        .cmpnyCd(tokenInfo.get("gv_cmpnyCd").toString())
                        .userId(tokenInfo.get("gv_userId").toString())
                        .siteCd(request.getSiteCd())
                        .fileType("002")     	// 002: 위험성평가
                        .itemCd("")
                        .fileName("")
                        .build();
    			
    			fileMgmtCd = fileMapper.selectFileMgmtCd(baseQuery);
    			
    			FileInfoSave queryWithKey = baseQuery.toBuilder()
                        .fileMgmtCd(fileMgmtCd)
                        .build();
    			
    			fileService.saveFile(queryWithKey, file);
    		}
    		
    		SaveAssessmentQry reqDto = SaveAssessmentQry.builder()
    				.siteCd(request.getSiteCd())
    				.assessmentCd(request.getAssessmentCd())
    				.assessmentStatus(request.getAssessmentStatus())
    				.processCd(request.getProcessCd())
    				.initLikelihoodScore(request.getInitLikelihoodScore())
    				.initSeverityScore(request.getInitSeverityScore())
    				.initRiskLv(request.getInitRiskLv())
    				.revalDate(request.getRevalDate())
    				.revalBeforeDesc(request.getRevalBeforeDesc())
    				.revalLikelihoodScore(request.getRevalLikelihoodScore())
    				.revalSeverityScore(request.getRevalSeverityScore())
    				.revalRiskLv(request.getRevalRiskLv())
    				.revalDesc(request.getRevalDesc())
    				.revalFileMgmtCd(fileMgmtCd)
    				.build();
    		
    		risk03Mapper.updateAssessment(reqDto, tokenInfo);
    		
		} catch (Exception e) {
			throw new RiskApiException(e.getMessage());
		}
	}

}
