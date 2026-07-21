package com.prafta.web.user.user03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.user.user03.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user03.application.query.SiteInfoListQuery;
import com.prafta.web.user.user03.result.SiteInfoResult;

@Mapper
public interface User03Mapper {
	List<SiteInfoResult> selectSiteInfoSearch(SiteInfoListQuery query);

	void mergeUserSiteAuth(UserSiteAuthCommand command);

	/** 대상 사용자 권한코드(AUTH_CD) 조회 — master/hr 사업장 권한 회수 차단 가드용. 미존재 시 null. */
	String selectUserAuthCd(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);
}
