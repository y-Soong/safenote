package com.prafta.web.leaverefusal.leaverefusal01.service;

import com.prafta.web.leaverefusal.leaverefusal01.application.param.LeaveRefusalNoticeParam;
import com.prafta.web.leaverefusal.leaverefusal01.dto.response.LeaveRefusalNoticeResponse;

/**
 * 노무수령거부 통지 발송 서비스 (PRAFTA-COM-001 기능1).
 */
public interface LeaveRefusalNoticeService {

    /**
     * 대상별로 outbox(PENDING) + 사실 로그(NOTICED)를 한 트랜잭션으로 적재한다. 멱등.
     *
     * @param param 통지 대상 목록 + JWT 식별자 (권한 게이트: master/hr)
     * @return 요청/신규 적재 건수
     */
    LeaveRefusalNoticeResponse sendNotices(LeaveRefusalNoticeParam param);
}
