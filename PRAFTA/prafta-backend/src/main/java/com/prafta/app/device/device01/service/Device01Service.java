package com.prafta.app.device.device01.service;

import com.prafta.app.device.device01.application.param.PushTokenParam;
import com.prafta.app.device.device01.dto.response.PushTokenResponse;

/**
 * 단말 푸시 토큰 등록 (앱) 서비스.
 * 로그인 직후/토큰 refresh 시 DEVICE_UUID 기준으로 tb_user_device.PUSH_TOKEN 을 upsert(실질 UPDATE)한다.
 */
public interface Device01Service {

    // 푸시 토큰 등록(upsert). 본인 단말(DEVICE_UUID + JWT USER_CD) PUSH_TOKEN/DEL_YN 갱신.
    PushTokenResponse upsertPushToken(PushTokenParam param);
}
