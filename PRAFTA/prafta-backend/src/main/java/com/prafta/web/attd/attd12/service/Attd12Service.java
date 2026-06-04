package com.prafta.web.attd.attd12.service;

import com.prafta.web.attd.attd12.application.param.FraudAttdSuspectParam;
import com.prafta.web.attd.attd12.dto.response.FraudAttdSuspectResponse;

/**
 * prafta-com-003 C6 - 부정 출퇴근 의심 탐지 서비스(on-view 대조, 읽기 전용·표시 전용).
 */
public interface Attd12Service {

    /** 규칙1(한 기기 다계정) + 보조2/3(평소/신규 기기) 의심 케이스 조회. */
    FraudAttdSuspectResponse getFraudAttdSuspects(FraudAttdSuspectParam param);
}
