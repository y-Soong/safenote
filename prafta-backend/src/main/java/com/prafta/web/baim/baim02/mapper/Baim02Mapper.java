package com.prafta.web.baim.baim02.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim02.dto.CompCmmCodeDListQry;
import com.prafta.web.baim.baim02.dto.CompCmmCodeDSave;
import com.prafta.web.baim.baim02.dto.CompCmmCodeMListQry;
import com.prafta.web.baim.baim02.vo.CompCmmCodeD;
import com.prafta.web.baim.baim02.vo.CompCmmCodeM;

@Mapper
public interface Baim02Mapper {
	List<CompCmmCodeM> selectCompCmmCodeMList(@Param(value = "param") CompCmmCodeMListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<CompCmmCodeD> selectCompCmmCodeDList(@Param(value = "param") CompCmmCodeDListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeCmmCodeDetailInfo(@Param(value = "param") CompCmmCodeDSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteCmmCodeDetailInfo(@Param(value = "param") CompCmmCodeDSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
