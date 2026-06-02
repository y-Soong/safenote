package com.prafta.web.baim.baim03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.baim.BaimErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim03.application.command.TermsInfoCommand;
import com.prafta.web.baim.baim03.application.model.TermsModel;
import com.prafta.web.baim.baim03.application.param.TermsDetailInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoParam;
import com.prafta.web.baim.baim03.application.param.TermsListParam;
import com.prafta.web.baim.baim03.application.query.TermsDetailInfoListQuery;
import com.prafta.web.baim.baim03.application.query.TermsInfoListQuery;
import com.prafta.web.baim.baim03.dto.response.TermsDetailInfoListResponse;
import com.prafta.web.baim.baim03.dto.response.TermsInfoListResponse;
import com.prafta.web.baim.baim03.mapper.Baim03Mapper;
import com.prafta.web.baim.baim03.result.TermsDetailInfoResult;
import com.prafta.web.baim.baim03.result.TermsInfoResult;
import com.prafta.web.baim.baim03.service.Baim03Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Baim03ServiceImpl implements Baim03Service{
	private final Baim03Mapper baim03Mapper;
	
	public Baim03ServiceImpl(Baim03Mapper baim03Mapper) {
		this.baim03Mapper = baim03Mapper;
	}
		
	public TermsInfoListResponse selectTermsList(TermsInfoListParam param) {
				
		TermsInfoListResponse response = null;
		
		List<TermsInfoResult> termsInfoList = baim03Mapper.selectTermsList(TermsInfoListQuery.from(param));
		
		if(termsInfoList != null && termsInfoList.size() > 0) {
			response = TermsInfoListResponse.builder()
									.termsInfoList(termsInfoList)
									.build();
		}
		
		return response;
	}

	public TermsDetailInfoListResponse selectTermsDList(TermsDetailInfoListParam param) {

		TermsDetailInfoListResponse response = null;
		
		List<TermsDetailInfoResult> termsDetailInfoList = baim03Mapper.selectTermsDList(TermsDetailInfoListQuery.from(param));
		
		if(termsDetailInfoList != null &&termsDetailInfoList.size() > 0) {
			response = TermsDetailInfoListResponse.builder()
									.termsDetailInfoList(termsDetailInfoList)
									.build();
		}
		
		return response;
	}
	
	@Transactional
	public void updateTermsInfo(TermsInfoParam param) {

		List<TermsInfoResult> termsInfoResultList = baim03Mapper.selectTermsList(TermsInfoListQuery.from(param));
		
		if (termsInfoResultList != null && !termsInfoResultList.isEmpty()) {
			
			TermsInfoResult last = termsInfoResultList.get(termsInfoResultList.size() - 1);

		    String lastVerStr = last.termsVersion();
		    int lastVer = 0;
		    try {
		        lastVer = Integer.parseInt(lastVerStr == null ? "0" : lastVerStr.trim());
		    } catch (NumberFormatException e) {
		    	throw new ApiException(CommonErrorCode.COMMON_400_001, "약관 버전 값이 숫자가 아닙니다. termsVersion=" + lastVerStr);
		    }

		    String versionNo = String.valueOf(lastVer + 1);
			
			baim03Mapper.mergeTermsInfo(TermsInfoCommand.from(param, versionNo));
			baim03Mapper.insertTermsIdVersionInfo(TermsInfoCommand.from(param, versionNo));
		} else {
			throw new ApiException(BaimErrorCode.BAIM_500_001);
		}
	}
	
	public void deleteCmmCodeDetailInfo(TermsListParam param) {
		for(TermsModel model : param.termsInfoModelList()) {

//			baim03Mapper.deleteCmmCodeDetailInfo(TermsInfoCommand.from(model, null));
		}
	}
	
}
