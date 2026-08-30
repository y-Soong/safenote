package com.prafta.web.user.user05.service;

import com.prafta.web.user.user05.application.param.DailyContractHistoryParam;
import com.prafta.web.user.user05.application.param.DailyUserListParam;
import com.prafta.web.user.user05.dto.response.DailyContractHistoryResponse;
import com.prafta.web.user.user05.dto.response.DailyUserListResponse;

public interface User05Service {

    /** 일일사용자 관리(조회) 목록. 만료 일일계정 포함, 사업장 스코프/페이징 적용. */
    DailyUserListResponse selectDailyUserList(DailyUserListParam param);

    /** 일일사용자 계약이력(서명 이력 + 입장 승인/로그인 이력). 대상 사업장 인가 재검증(IDOR 차단). */
    DailyContractHistoryResponse selectDailyContractHistory(DailyContractHistoryParam param);
}
