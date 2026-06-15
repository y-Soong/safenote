package com.prafta.app.device.device01.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 푸시 토큰 등록 요청 본문(JSON @RequestBody).
 *
 * <p>식별자(USER_CD/CMPNY_CD)는 본문에서 받지 않고 JWT 클레임에서만 도출한다(IDOR 차단).
 * <p>deviceId(DEVICE_UUID)는 axios 인터셉터가 모든 요청에 자동 동봉하는 gv_deviceId 키로 도착한다
 *    (com-003 동일 키). 신뢰경계 밖(위조 가능)이나 WHERE 절에 JWT USER_CD 를 함께 강제하므로
 *    타 유저 단말의 토큰을 변조할 수 없다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PushTokenRequest {
    private String pushToken;   // FCM 푸시 토큰(필수)
    private String platform;    // 플랫폼('android') — 현재 DEVICE_TYPE 갱신엔 미사용, 향후 확장 보조
    // com-003 과 동일 키: axios 인터셉터가 전 요청에 동봉. DEVICE_UUID 로 사용.
    @JsonProperty("gv_deviceId")
    private String deviceId;
}
