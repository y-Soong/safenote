package com.prafta.web.user.user01.service;

import com.prafta.web.user.user01.application.param.UserInfoParam;
import com.prafta.web.user.user01.dto.UserBatchUpdateResponse;

public interface User01BatchService {
    UserBatchUpdateResponse updateUserInfoBatch(UserInfoParam param);
}