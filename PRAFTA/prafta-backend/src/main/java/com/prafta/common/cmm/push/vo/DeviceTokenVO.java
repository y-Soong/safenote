package com.prafta.common.cmm.push.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 대상 사용자의 FCM 디바이스 토큰 1건 (PRAFTA-COM-002).
 *
 * <p>{@code tb_user_device} 에서 {@code USER_CD} 로 조회한 활성(DEL_YN='N') 디바이스.
 * 무효 토큰(FCM UNREGISTERED/INVALID_ARGUMENT) 응답 시 {@code deviceUuid} 로
 * soft-delete(DEL_YN='Y') 한다. 한 사용자 다중 디바이스 가능.
 */
@Getter
@Setter
public class DeviceTokenVO {

    /** 디바이스 UUID (PK, 무효 토큰 soft-delete 키) */
    private String deviceUuid;

    /** FCM 푸시 토큰 (전송 대상) */
    private String pushToken;
}
