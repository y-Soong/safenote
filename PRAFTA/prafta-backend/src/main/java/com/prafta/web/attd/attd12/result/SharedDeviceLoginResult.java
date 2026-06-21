package com.prafta.web.attd.attd12.result;

/**
 * prafta-com-016-F 9-1 - 공유 기기 의심 로그인 1행(login_hist 기반).
 *
 * <p>한 DEVICE_UUID 를 직전 7일 내에 서로 다른 사용자가 APP 로그인한 "공유 기기"에 대해,
 *   그 기기에 관여한 로그인 이력(사용자·시각·기기 메타)을 평탄(flat) 행으로 내려준다.
 *   서비스에서 DEVICE_UUID 로 그룹핑하여 기기 중심(1 기기 → N 사용자) 표시 모델로 조립한다.
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 *   selectSharedDeviceLoginSuspects 의 SELECT 절 컬럼 순서와 본 record 필드 순서를 1:1 로 유지할 것
 *   (feedback_mybatis_record_column_order).
 */
public record SharedDeviceLoginResult(
        String deviceUuid        // 공유 의심 기기 UUID(NOT NULL)
        , String userCd
        , String userId
        , String userNm
        , String nodeNm          // 부서명(USER_CD → TB_USER → node)
        , String siteNm          // 사업장명
        , String loginDtime      // 로그인 일시 YYYYMMDDHHMMSS
        , String clientType      // APP / WEB
        , String deviceModel     // 기기 모델(nullable)
        , String osVersion       // OS 버전(nullable)
) {
}
