package com.prafta.web.user.user05.service;

import com.prafta.web.user.user05.application.param.DailyUserListParam;
import com.prafta.web.user.user05.dto.response.DailyUserListResponse;

public interface User05Service {

    /** 일일사용자 관리(조회) 목록. 만료 일일계정 포함, 사업장 스코프/페이징 적용. */
    DailyUserListResponse selectDailyUserList(DailyUserListParam param);
}
