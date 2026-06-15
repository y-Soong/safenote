package com.prafta.app.device.device01.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.app.device.device01.application.command.UpsertPushTokenCommand;
import com.prafta.app.device.device01.application.param.PushTokenParam;
import com.prafta.app.device.device01.dto.response.PushTokenResponse;
import com.prafta.app.device.device01.mapper.Device01Mapper;
import com.prafta.app.device.device01.service.Device01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 단말 푸시 토큰 등록 (앱) 서비스 구현.
 *
 * <p>USER_CD 는 Param 단계에서 JWT 클레임으로만 확정되며, 본문 도착 DEVICE_UUID 와 함께
 *    WHERE 절(DEVICE_UUID + USER_CD)로 강제하여 타 유저 단말 토큰 변조를 차단한다(IDOR).
 * <p>정상 흐름은 로그인 시 LoginMapper.upsertUserDevice 가 DEVICE_UUID 행을 선행 생성하므로 UPDATE 가 성립한다.
 *    영향행이 0이면(행 부재) 경고 로깅 후 성공 ack 를 반환한다(클라 로그인 플로우 차단 금지, F01 §6).
 * <p>로깅 시 PUSH_TOKEN/DEVICE_UUID 는 앞 8자 + *** 로 마스킹한다(평문 금지, S2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Device01ServiceImpl implements Device01Service {

    private final Device01Mapper device01Mapper;

    @Override
    @Transactional
    public PushTokenResponse upsertPushToken(PushTokenParam param) {

        String maskedDevice = mask(param.deviceUuid());
        String maskedToken = mask(param.pushToken());

        log.info("푸시 토큰 등록 진입 - userCd={}, deviceUuid={}, pushToken={}"
            , param.gvUserCd(), maskedDevice, maskedToken);

        int affected = device01Mapper.updatePushToken(UpsertPushTokenCommand.from(param));

        if (affected == 0) {
            // 행 부재: 로그인 upsert 가 선행되지 않은 비정상 경로. 클라 차단 없이 성공 ack(경고만).
            log.warn("푸시 토큰 대상 단말 행 없음(영향행 0) - userCd={}, deviceUuid={}"
                , param.gvUserCd(), maskedDevice);
        } else {
            log.info("푸시 토큰 등록 완료 - userCd={}, deviceUuid={}, 영향행={}"
                , param.gvUserCd(), maskedDevice, affected);
        }

        return PushTokenResponse.success();
    }

    /**
     * 식별자/토큰 마스킹: 앞 8자 + ***. 평문 로그 금지(S2).
     * 8자 미만이면 길이만큼 노출 후 ***(짧은 값도 평문 전체노출 방지).
     */
    private String mask(String value) {
        if (value == null || value.isBlank())
            return "(none)";
        int head = Math.min(8, value.length());
        return value.substring(0, head) + "***";
    }
}
