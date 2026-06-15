package com.prafta.web.risk.risklink01.service;

import com.prafta.web.risk.risklink01.application.param.AvailableNearMissParam;
import com.prafta.web.risk.risklink01.application.param.LinkedNearMissParam;
import com.prafta.web.risk.risklink01.application.param.NearMissLinkParam;
import com.prafta.web.risk.risklink01.dto.response.NearMissLinkListResponse;

public interface RiskLink01Service {

    // L1 연결 후보 아차사고 검색 (완료400 + 같은 사업장 + 미연결)
    NearMissLinkListResponse selectAvailableNearMiss(AvailableNearMissParam param);

    // L2 연결된 아차사고 목록 (USE_YN='Y')
    NearMissLinkListResponse selectLinkedNearMiss(LinkedNearMissParam param);

    // L3 연결 추가 (개선완료003 전 + 완료400 아차사고 검증 + upsert)
    void linkNearMiss(NearMissLinkParam param);

    // L4 연결 해제 (개선완료003 전 + soft delete)
    void unlinkNearMiss(NearMissLinkParam param);
}
