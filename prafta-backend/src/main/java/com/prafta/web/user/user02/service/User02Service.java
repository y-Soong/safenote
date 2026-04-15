package com.prafta.web.user.user02.service;

import com.prafta.web.user.user02.application.param.AuthMenuInfoParam;
import com.prafta.web.user.user02.application.param.AuthMenuListParam;
import com.prafta.web.user.user02.dto.response.AuthMenuListResponse;

public interface User02Service {
	AuthMenuListResponse selectAuthMenuList(AuthMenuListParam param);
	
	void updateAuthMenuInfo(AuthMenuInfoParam param);
}
