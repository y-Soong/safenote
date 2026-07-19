package com.prafta.platform.location.application.result;

import java.math.BigDecimal;

/**
 * 위치정보 조회 결과 1행(TB_USER_ATTD_GPS + TBM 입실 UNION).
 *
 * <p>PII 최소화(요청서 §5-3): USER_CD 만 포함 — 실명/휴대폰 등 평문 미포함.
 * ⚠️ UNION 양쪽 SELECT 컬럼 순서/타입은 본 컴포넌트 순서와 완전 일치 유지(record 매핑 함정).
 */
public record GpsInfoResult(
    String srcType        // 수집원 ATTD:근태 GPS / TBM:TBM 입실
    , String userCd       // 사용자코드(TBM DAILY 는 TB_DAILY_USER 의 USER_CD 일 수 있음)
    , String measureTime  // 측정시각(HHmmss)
    , String gpsInfoType  // GPS정보타입[SYS028] 01:출근 02:퇴근 (TBM 은 NULL)
    , BigDecimal lat
    , BigDecimal lon
    , BigDecimal accuracy // 정확도(m, TBM 은 NULL)
    , String mockedYn     // Mock위치여부 Y/N (TBM 은 수집 시 미판정 — 'N' 고정, 화면 '-' 표시)
    , String ipAddr       // 수집 시 IP(TBM 은 NULL)
) {
}
