package com.prafta.app.safety.history.service;

import com.prafta.app.safety.history.application.param.MySafetyHistoryParam;
import com.prafta.app.safety.history.dto.response.MySafetyHistoryResponse;

/**
 * 내 안전활동 이력 서비스 (prafta-app-025 J1-10 B-6).
 */
public interface AppMySafetyHistoryService {

    /** 본인 안전활동 이력(순회점검 + 위험성평가) 시간순 병합 + 페이징 슬라이스. */
    MySafetyHistoryResponse selectMyHistory(MySafetyHistoryParam param);
}
