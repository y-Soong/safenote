package com.prafta.web.nearmiss.nearmiss01.service;

import com.prafta.web.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentInfoParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentListParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.SaveIncidentParam;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.StatusCountResponse;

public interface NearMiss01Service {

    // E1 사건 목록
    IncidentListResponse selectIncidentList(IncidentListParam param);

    // E2 사건 단건 상세
    IncidentInfoResponse selectIncidentInfo(IncidentInfoParam param);

    // E3 상태별 카운트
    StatusCountResponse selectStatusCounts(IncidentListParam param);

    // E4 정밀조사 저장
    void saveIncident(SaveIncidentParam param);

    // E5 상태 전환
    void changeStatus(ChangeStatusParam param);
}
