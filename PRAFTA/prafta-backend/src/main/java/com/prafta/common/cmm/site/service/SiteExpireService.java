package com.prafta.common.cmm.site.service;

/**
 * PRAFTA-COM-001-T2-3 — 사업장 만료 일괄 비활성 서비스.
 */
public interface SiteExpireService {

    /**
     * 종료일이 도래(END_DATE &lt;= 오늘) AND 활성(USE_YN='Y') 인 사업장을 USE_YN='N' 으로 일괄 전이한다(멱등).
     *
     * @return 전이된 행 수
     */
    int expireOverdueSites();
}
