package com.prafta.web.attd.attd12.result;

/**
 * prafta-com-003 C6 - 스코프(사업장/부서) 내 한 달치 출퇴근 행(부정탐지 원천).
 *
 * <p>⚠️ MyBatis record 매핑은 SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 *   selectScopedAttdRows 의 SELECT 절 컬럼 순서와 본 record 필드 순서를 1:1 로 유지할 것
 *   (feedback_mybatis_record_column_order).
 *
 * <p>CHECK_IN_DEVICE_UUID 가 NULL 인 행(웹 등록/구버전 앱)은 규칙1 그룹 대상에서 제외된다.
 */
public record FraudAttdRowResult(
        String workYmd          // 근무일 YYYYMMDD
        , String userCd
        , String userId
        , String userNm
        , String nodeNm         // 부서명
        , String siteNm         // 사업장명
        , String checkInTime    // HHMM
        , String checkOutTime   // HHMM (nullable)
        , String checkInDeviceUuid   // 출근 실행 기기(nullable)
        , String checkOutDeviceUuid  // 퇴근 실행 기기(nullable)
) {
}
