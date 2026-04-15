package com.prafta.web.baim.baim02.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim02.application.command.CompCmmCodeDCommand;
import com.prafta.web.baim.baim02.application.query.CompCmmCodeDListQuery;
import com.prafta.web.baim.baim02.application.query.CompCmmCodeMListQuery;
import com.prafta.web.baim.baim02.result.CompCmmCodeDResult;
import com.prafta.web.baim.baim02.result.CompCmmCodeMResult;

@Mapper
public interface Baim02Mapper {
	List<CompCmmCodeMResult> selectCompCmmCodeMList(CompCmmCodeMListQuery query);
	
	List<CompCmmCodeDResult> selectCompCmmCodeDList(CompCmmCodeDListQuery query);
	
	void mergeCmmCodeDetailInfo(CompCmmCodeDCommand command);
	
	void deleteCmmCodeDetailInfo(CompCmmCodeDCommand command);
}
