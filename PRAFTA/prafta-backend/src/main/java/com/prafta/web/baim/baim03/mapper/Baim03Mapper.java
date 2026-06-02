package com.prafta.web.baim.baim03.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim03.application.command.TermsInfoCommand;
import com.prafta.web.baim.baim03.application.query.TermsDetailInfoListQuery;
import com.prafta.web.baim.baim03.application.query.TermsInfoListQuery;
import com.prafta.web.baim.baim03.result.TermsDetailInfoResult;
import com.prafta.web.baim.baim03.result.TermsInfoResult;

@Mapper
public interface Baim03Mapper {
	List<TermsInfoResult> selectTermsList(TermsInfoListQuery query);
	
	List<TermsDetailInfoResult> selectTermsDList(TermsDetailInfoListQuery query);
	
	void mergeTermsInfo(TermsInfoCommand command);
	
	void insertTermsIdVersionInfo(TermsInfoCommand command);
	
	void deleteCmmCodeDetailInfo(@Param(value = "param") TermsInfoCommand dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
