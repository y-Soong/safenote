package com.prafta.app.leave.leave01.service;

import com.prafta.app.leave.leave01.application.param.MyLeaveSummaryParam;
import com.prafta.app.leave.leave01.dto.response.MyLeaveSummaryResponse;

/**
 * prafta-app-005: 앱 "연차 현황"(본인 잔여연차 상세) 서비스 인터페이스.
 */
public interface AppLeave01Service {

    /**
     * 본인 연차 현황(그룹 3종 + 소멸임박 + 사용자 메타) 단일 조회.
     */
    MyLeaveSummaryResponse selectMyLeaveSummary(MyLeaveSummaryParam param);
}
