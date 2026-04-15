package com.prafta.common.cmm.auth.service;

import com.prafta.common.cmm.auth.application.param.RefreshParam;
import com.prafta.common.cmm.auth.dto.response.RefreshResponse;

public interface AuthService {
	RefreshResponse refreshAccessToken(RefreshParam param);
}
