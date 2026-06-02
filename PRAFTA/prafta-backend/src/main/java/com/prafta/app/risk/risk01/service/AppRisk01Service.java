package com.prafta.app.risk.risk01.service;

import com.prafta.app.risk.risk01.application.param.RiskAssessmentSaveParam;
import com.prafta.app.risk.risk01.application.param.RiskTypeInfoParam;
import com.prafta.app.risk.risk01.dto.response.RiskTypeInfoResponse;

/**
 * prafta-036-B2: 앱 위험성평가(risk01) 서비스 인터페이스.
 */
public interface AppRisk01Service {

    /**
     * 위험성평가 구분/분류/발생상황 정보 조회.
     */
    RiskTypeInfoResponse selectRiskTypeInfo(RiskTypeInfoParam param);

    /**
     * 위험성평가 저장 (multipart, 단일 파일).
     */
    void saveRiskAssessments(RiskAssessmentSaveParam param);
}
