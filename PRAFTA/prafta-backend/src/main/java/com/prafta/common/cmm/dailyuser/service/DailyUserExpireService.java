package com.prafta.common.cmm.dailyuser.service;

/**
 * PRAFTA-app-027-1 — 일용직 만료 계정 일괄 비활성 서비스.
 */
public interface DailyUserExpireService {

    /**
     * 만료(WORK_EXPIRE_DATE &lt; 오늘) AND 활성(USE_YN='Y') 일용직을
     * USE_YN='N' + ACCOUNT_STATUS='05'로 일괄 전이한다(멱등).
     *
     * @return 전이된 행 수
     */
    int expireOverdueDailyUsers();
}
