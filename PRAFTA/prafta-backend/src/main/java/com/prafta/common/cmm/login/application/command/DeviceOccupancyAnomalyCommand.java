package com.prafta.common.cmm.login.application.command;

/**
 * prafta-com-015 015-1 — 디바이스 점유 재할당 이상 적재 커맨드.
 *
 * <p>한 DEVICE_UUID 의 현재 점유자(USER_CD)가 "다른 계정"으로 바뀌는 로그인을 감지했을 때
 * {@code insertOccupancyAnomaly} 로 감사 테이블(tb_user_device_occupancy_anomaly)에 적재한다.
 * ANOMALY_NO(PK)는 매퍼에서 FNC_CMM_SEQ_NEXTVAL 로 채번한다.
 *
 * <p>deviceUuid 는 클라 제공값(신뢰경계 밖). loginIp 는 서버가 HttpServletRequest 에서 추출한 값.
 * iOS IDFV 변경/재설치(=동일 USER_CD 재로그인)는 호출 측에서 게이트되어 적재되지 않는다.
 * 적재는 로그인 성공 직후 예외 격리 블록에서 best-effort 로 수행한다(실패해도 로그인 영향 없음).
 */
public record DeviceOccupancyAnomalyCommand(
    String cmpnyCd
    , String deviceUuid    // 클라 제공 디바이스UUID
    , String prevUserCd    // 직전 점유자(재할당 전 USER_CD)
    , String newUserCd     // 로그인 사용자(재할당 후 USER_CD)
    , String clientType    // 'APP' / 'WEB' / null
    , String loginIp       // 서버 추출 IP / null
) {
}
