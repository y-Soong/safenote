package com.prafta.common.cmm.login.application.command;

/**
 * prafta-com-003 C3: 로그인 성공 시 디바이스 적재 커맨드.
 *
 * <p>두 곳에서 공통 사용한다:
 *   <ul>
 *     <li>{@code upsertUserDevice} — tb_user_device(현재 상태 1행, PK DEVICE_UUID) ON DUPLICATE KEY UPDATE.
 *         재로그인 시 USER_CD/메타/LAST_LOGIN_* 갱신 + (com-002) DEL_YN='N' 으로 되살림.</li>
 *     <li>{@code insertDeviceLoginHist} — tb_user_device_login_hist(append-only) INSERT.
 *         PK(DEVICE_LOGIN_NO)는 매퍼에서 FNC_CMM_SEQ_NEXTVAL 로 채번한다.</li>
 *   </ul>
 *
 * <p>deviceId/메타는 클라 제공값(신뢰경계 밖). LOGIN_IP 는 서버가 HttpServletRequest 에서 추출한 값.
 *   적재는 로그인 성공 직후 예외 격리 블록에서 best-effort 로 수행한다(실패해도 로그인 영향 없음).
 */
public record DeviceLoginCommand(
    String cmpnyCd
    , String deviceUuid    // 클라 제공 디바이스UUID (PK 후보)
    , String userCd
    , String deviceType    // 'ANDROID' / 'IOS' / null
    , String deviceModel
    , String osVersion
    , String appVersion
    , String clientType    // 'APP' / 'WEB'
    , String loginIp       // 서버 추출 IP
    , String insertNo      // userCd
) {
}
