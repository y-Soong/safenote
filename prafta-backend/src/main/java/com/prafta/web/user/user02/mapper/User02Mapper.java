package com.prafta.web.user.user02.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user02.dto.User02;
import com.prafta.web.user.user02.dto.User02ReqDto;

@Mapper
public interface User02Mapper {
	List<Map<String, Object>> selectAuthMenuList(@Param(value = "param") User02ReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeAuthMenuInfo(@Param(value = "param") User02 dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
