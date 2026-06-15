package com.prafta.app.device.device01.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.app.device.device01.application.command.UpsertPushTokenCommand;

/**
 * 단말 푸시 토큰 등록 (앱) Mapper.
 *
 * <p>tb_user_device 의 PUSH_TOKEN 갱신만 담당한다(스키마 변경 없음).
 *    DEVICE_UUID(PK) 행은 로그인 시 LoginMapper.upsertUserDevice 가 선행 생성하므로 정상 흐름은 UPDATE.
 */
@Mapper
public interface Device01Mapper {

    // 푸시 토큰 UPDATE (WHERE DEVICE_UUID + USER_CD 강제, DEL_YN='N' 회복, UPDATE_NO/UPDATE_DATE 갱신).
    //   영향행 수 반환(0 이면 본인 단말 행 부재 → 서비스에서 경고 로깅 후 성공 ack).
    int updatePushToken(UpsertPushTokenCommand command);
}
