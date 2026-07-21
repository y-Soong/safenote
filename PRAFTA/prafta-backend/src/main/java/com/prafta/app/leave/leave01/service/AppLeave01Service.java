package com.prafta.app.leave.leave01.service;

import com.prafta.app.leave.leave01.application.param.MyLeaveSummaryParam;
import com.prafta.app.leave.leave01.application.param.MyLeaveUseListParam;
import com.prafta.app.leave.leave01.dto.response.MyLeaveSummaryResponse;
import com.prafta.app.leave.leave01.dto.response.MyLeaveUseListResponse;

/**
 * prafta-app-005: 앱 "연차 현황"(본인 잔여연차 상세) 서비스 인터페이스.
 */
public interface AppLeave01Service {

    /**
     * 본인 연차 현황(그룹 3종 + 소멸임박 + 사용자 메타) 단일 조회.
     */
    MyLeaveSummaryResponse selectMyLeaveSummary(MyLeaveSummaryParam param);

    /**
     * 본인 연차 사용 내역(연 단위) 조회 — 연차 현황 화면 하단 리스트.
     * year 미지정이면 DB 기준 올해로 보정한다.
     */
    MyLeaveUseListResponse selectMyLeaveUses(MyLeaveUseListParam param);
}
