package com.prafta.web.attd.attd08.result;

import java.math.BigDecimal;

/**
 * GPS좌표-암호화-전환-03: GPS 동선 조회 내부 행(암호문+평문 병렬 — fallback 복호화 전 원천).
 *
 * <p>서비스 계층(Attd08ServiceImpl)이 latEnc/lonEnc 우선 복호화, NULL 이면 구 평문 lat/lon 을
 * 사용해 기존 {@link AttdGpsTrailResult} 로 변환한다(응답 계약 불변). 매퍼에 복호화 로직 금지.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — selectAttdGpsTrail 의 SELECT 순서와
 * 본 컴포넌트 순서를 완전 일치 유지할 것.
 */
public record AttdGpsTrailRow(
      String gpsId
    , String latEnc        // 위도 암호문(AES-GCM v1., NULL=백필 전 구 평문 행)
    , String lonEnc        // 경도 암호문
    , BigDecimal lat       // 구 평문 위도(전환기 fallback — 소거 후 NULL)
    , BigDecimal lon       // 구 평문 경도
    , BigDecimal accuracy
    , String apiCallDate
    , String apiCallTime
    , String isMocked
    , String gpsInfoType
) {
}
