package com.prafta.web.user.user01.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.dto.TokenInfo;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.application.param.UserInfoParam;
import com.prafta.web.user.user01.dto.UserBatchUpdateResponse;

public interface User01BatchService {
    UserBatchUpdateResponse updateUserInfoBatch(UserInfoParam param);

    // ===== PRAFTA-036 - 엑셀 일괄 사용자 생성 =====
    UserBatchUpdateResponse insertUserBatch(List<UserCreateParam> params);

    UserBatchUpdateResponse uploadUserCreates(MultipartFile file, TokenInfo tokenInfo);
}
