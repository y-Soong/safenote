package com.prafta.web.user.user03.service;

import com.prafta.web.user.user03.application.param.SiteInfoListParam;
import com.prafta.web.user.user03.application.param.UserSiteAuthParam;
import com.prafta.web.user.user03.dto.response.SiteInfoListResponse;

public interface User03Service {
	SiteInfoListResponse selectSiteInfoSearch(SiteInfoListParam param);
	
	void updateUserSiteAuth(UserSiteAuthParam param);
}
