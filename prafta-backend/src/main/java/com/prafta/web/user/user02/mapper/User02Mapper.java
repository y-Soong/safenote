package com.prafta.web.user.user02.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user02.dto.AuthMenuInfoQry;
import com.prafta.web.user.user02.dto.AuthMenuListQry;
import com.prafta.web.user.user02.vo.AuthMenu;

@Mapper
public interface User02Mapper {
	List<AuthMenu> selectAuthMenuList(@Param(value = "param") AuthMenuListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeAuthMenuInfo(@Param(value = "param") AuthMenuInfoQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
