package com.prafta.common.cmm.dailylogin.service;

import com.prafta.common.cmm.dailylogin.application.param.DailyLoginParam;
import com.prafta.common.cmm.dailylogin.dto.response.DailyLoginResponse;

/**
 * PRAFTA-app-027-2 — 일용직 직접 로그인 서비스.
 */
public interface DailyLoginService {

    /**
     * 일용직 아이디/비밀번호 로그인. USE_YN='Y' + ACCOUNT_STATUS!='05' + 탈퇴 아님 + 비번 일치 시
     * 일용직 전용 정식 토큰 + 리프레시 토큰을 발급한다. 차단은 통합 메시지로 처리한다.
     */
    DailyLoginResponse login(DailyLoginParam param);
}
