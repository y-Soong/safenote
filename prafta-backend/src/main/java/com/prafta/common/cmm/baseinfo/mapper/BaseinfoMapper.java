package com.prafta.common.cmm.baseinfo.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.baseinfo.dto.BaseInfoListQuery;
import com.prafta.common.cmm.baseinfo.dto.BaseinfoReq;
import com.prafta.common.cmm.baseinfo.dto.SiteNodeListQry;
import com.prafta.common.cmm.baseinfo.dto.SystInfoListQuery;
import com.prafta.common.cmm.baseinfo.dto.SystInfoQry;
import com.prafta.common.cmm.baseinfo.vo.BaseInfo;
import com.prafta.common.cmm.baseinfo.vo.SiteNodeInfo;
import com.prafta.common.cmm.baseinfo.vo.SystInfo;

@Mapper
public interface BaseinfoMapper {
	List<SystInfo> selectSystinfoList(SystInfoListQuery reqDto);
	
	List<SystInfo> selectSystinfo(SystInfoQry dto);
	
	List<BaseInfo> selectBaseinfoList(BaseInfoListQuery reqDto);
	
	Map<String, Object> selectCmpnyInfo(BaseinfoReq dto);
	
	Map<String, Object> selectUserIdDupleChk(BaseinfoReq dto);
	
	int selectMblUniqChk(BaseinfoReq dto);
	
	void insertSmsAuthReq(BaseinfoReq dto);
	
	int updateSmsAuthReq(BaseinfoReq dto);
	
	Map<String, Object> selectCertNoSmsId(BaseinfoReq dto);
	
	List<Map<String, Object>> selectSiteInfoList(BaseinfoReq dto);
	
	List<SiteNodeInfo> selectSiteNodeList(@Param("param") SiteNodeListQry dto, @Param("token") Map<String, Object> tokenInfo);
	
	List<Map<String, Object>> selectWebMenuList(BaseinfoReq dto);
	
	List<Map<String, Object>> selectAppMenuList(BaseinfoReq dto);
	
	List<Map<String, Object>> selectMenuList(BaseinfoReq dto);
	
	Map<String, Object> selectUserIdInfo(BaseinfoReq dto);
	
	void updateUserPw(BaseinfoReq dto);
	
	Map<String, Object> selectTermsDInfo(BaseinfoReq dto);
}
