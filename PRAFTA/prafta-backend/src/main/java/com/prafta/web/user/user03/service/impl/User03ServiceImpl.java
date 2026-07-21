package com.prafta.web.user.user03.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.user.UserErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.user.user03.application.command.UserSiteAuthCommand;
import com.prafta.web.user.user03.application.model.UserSiteAuthModel;
import com.prafta.web.user.user03.application.param.SiteInfoListParam;
import com.prafta.web.user.user03.application.param.UserSiteAuthParam;
import com.prafta.web.user.user03.application.query.SiteInfoListQuery;
import com.prafta.web.user.user03.dto.response.SiteInfoListResponse;
import com.prafta.web.user.user03.mapper.User03Mapper;
import com.prafta.web.user.user03.result.SiteInfoResult;
import com.prafta.web.user.user03.service.User03Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class User03ServiceImpl implements User03Service{
	private final User03Mapper user03Mapper;
	
	public User03ServiceImpl(User03Mapper user03Mapper) {
		this.user03Mapper = user03Mapper;
	}
	
	
	public SiteInfoListResponse selectSiteInfoSearch(SiteInfoListParam param) {
		
		SiteInfoListResponse response = null;
		
		List<SiteInfoResult> siteInfoList = user03Mapper.selectSiteInfoSearch(SiteInfoListQuery.from(param));
		
		if(siteInfoList != null && siteInfoList.size() > 0) {
			response = SiteInfoListResponse.builder()
									.siteInfoList(siteInfoList)
									.build();
		}
		
		return response;
	}

	@Transactional
	public void updateUserSiteAuth(UserSiteAuthParam param) {
		// master/hr 사업장 권한 회수 차단 — 전 사업장 권한 보유 불변식(SiteAccessService 전사 통과와 정합).
		//   회수(allocYn != 'Y') 요청만 검사한다(fail-closed). 부여('Y')는 제한 없음.
		//   merge 이전에 전 행을 선검증해 부분 반영을 막고, @Transactional 로 루프 중간 실패 시 전체 롤백한다.
		for (UserSiteAuthModel model : param.userSiteAuthModelList()) {
			if (!"Y".equals(model.allocYn())) {
				String targetAuthCd = user03Mapper.selectUserAuthCd(model.gvCmpnyCd(), model.userCd());
				if (AuthRoleUtils.isManager(targetAuthCd)) {
					log.warn("master/hr 사업장 권한 회수 차단 - cmpnyCd={}, targetUserCd={}, targetAuthCd={}, siteCd={}",
							model.gvCmpnyCd(), model.userCd(), targetAuthCd, model.siteCd());
					throw new ApiException(UserErrorCode.USER_400_072);
				}
			}
		}
		for (UserSiteAuthModel model : param.userSiteAuthModelList()) {
			user03Mapper.mergeUserSiteAuth(UserSiteAuthCommand.from(model));
		}
	}
	
}
