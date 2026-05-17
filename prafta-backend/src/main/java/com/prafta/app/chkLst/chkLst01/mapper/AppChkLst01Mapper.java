package com.prafta.app.chkLst.chkLst01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoQry;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoSave;
import com.prafta.app.chkLst.chkLst01.vo.ChecklistInfo;
import com.prafta.common.dto.TokenInfo;

@Mapper
public interface AppChkLst01Mapper {
	List<ChecklistInfo> selectChkLstInfo(@Param(value = "param") ChecklistInfoQry reqDto, @Param(value = "token") TokenInfo tokenInfo);

	void mergeChkptInspectAnswer(@Param(value = "param") ChecklistInfoSave reqDto, @Param(value = "token") TokenInfo tokenInfo);
}
