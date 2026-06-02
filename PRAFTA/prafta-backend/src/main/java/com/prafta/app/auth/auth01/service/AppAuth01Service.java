package com.prafta.app.auth.auth01.service;

import com.prafta.app.auth.auth01.application.param.WithdrawParam;
import com.prafta.app.auth.auth01.dto.response.WithdrawResponse;

/**
 * prafta-app-010-07: 회원 탈퇴 서비스.
 */
public interface AppAuth01Service {

    WithdrawResponse withdraw(WithdrawParam param);
}
