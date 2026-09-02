package com.prafta.web.location.location01.service;

import com.prafta.web.location.location01.application.param.LocationConsentStatusParam;
import com.prafta.web.location.location01.dto.response.LocationConsentHistResponse;
import com.prafta.web.location.location01.dto.response.LocationConsentStatusResponse;

/** 위치정보 동의 현황(Location_01) 조회 서비스 — 위치정보 동의철회·중지 S5. */
public interface Location01Service {

    /** 회사·사업장 내 사용자별 위치정보 동의 현재 상태 목록. */
    LocationConsentStatusResponse selectConsentStatusList(LocationConsentStatusParam param);

    /** 특정 사용자의 동의 전이 이력 + 파기 이력. */
    LocationConsentHistResponse selectConsentHistories(LocationConsentStatusParam param, String targetUserCd);
}
