package com.prafta.common.cmm.sms.impl;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.sms.SmsSendRequest;
import com.prafta.common.cmm.sms.SmsSendResult;
import com.prafta.common.cmm.sms.SmsSender;
import com.prafta.common.cmm.sms.client.PpurioClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SmsSender} 의 뿌리오 구현.
 *
 * <p>★게이트 OFF(또는 계정/인증키/발신번호 미설정)일 때 <b>예외를 던지지 않고</b>
 *    {@link SmsSendResult#skipped()} 를 반환한다. {@code HolidayApiClient} 식으로 생성자에서
 *    {@code IllegalStateException} 을 던지면 키 미설정 환경의 부팅이 깨진다(요청서 §4-2 명시 금지).
 *    {@code FcmClientImpl} / {@code LlmAnswerClient} 의 "조용히 비활성" 철학을 따른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PpurioSmsSender implements SmsSender {

    private final PpurioClient ppurioClient;

    @Override
    public boolean isEnabled() {
        return ppurioClient.isEnabled();
    }

    @Override
    public SmsSendResult send(SmsSendRequest request) {
        if (!isEnabled()) {
            return SmsSendResult.skipped();
        }
        // 실패도 예외가 아닌 결과로 돌아온다(디스패처가 기록 후 사용자 응답을 결정).
        return ppurioClient.send(request);
    }
}
