package com.prafta.platform.location.application.result;

import java.math.BigDecimal;

/**
 * GPS좌표-암호화-전환-04: 위치정보 UNION 조회 내부 행(암호문+평문 병렬 — fallback 복호화 전 원천).
 *
 * <p>서비스 계층(PlatformLocationServiceImpl)이 latEnc/lonEnc 우선 복호화, NULL 이면 구 평문
 * lat/lon 을 사용해 기존 {@link GpsInfoResult} 로 변환한다(응답 계약 불변). 매퍼에 복호화 로직 금지.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — selectGpsList UNION 양쪽 SELECT 컬럼 순서/타입과
 * 본 컴포넌트 순서를 완전 일치 유지할 것.
 */
public record GpsInfoRow(
    String srcType        // 수집원 ATTD:근태 GPS / TBM:TBM 입실
    , String userCd
    , String measureTime  // 측정시각(HHmmss)
    , String gpsInfoType  // GPS정보타입[SYS028] 01:출근 02:퇴근 (TBM 은 NULL)
    , String latEnc       // 위도 암호문(AES-GCM v1., NULL=백필 전 구 평문 행)
    , String lonEnc       // 경도 암호문
    , BigDecimal lat      // 구 평문 위도(전환기 fallback — 소거 후 NULL)
    , BigDecimal lon      // 구 평문 경도
    , BigDecimal accuracy // 정확도(m, TBM 은 NULL)
    , String mockedYn     // Mock위치여부 Y/N (TBM 은 'N' 고정)
    , String ipAddr       // 수집 시 IP(TBM 은 NULL)
) {
}
