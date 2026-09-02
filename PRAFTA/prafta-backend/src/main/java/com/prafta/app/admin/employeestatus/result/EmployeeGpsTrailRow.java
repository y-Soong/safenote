package com.prafta.app.admin.employeestatus.result;

import java.math.BigDecimal;

/**
 * PRAFTA-003: GPS 동선 조회 내부 행(암호문+평문 병렬 — fallback 복호화 전 원천).
 *
 * <p>웹 {@code Attd08Mapper.selectAttdGpsTrail}/{@code AttdGpsTrailRow}를 앱 패키지로 그대로 이식했다
 * (SQL 재설계 금지 — plan §0-2/PRAFTA-003 상세). 서비스 계층({@code AppAdminEmployeeStatusServiceImpl})이
 * latEnc/lonEnc 우선 복호화, NULL 이면 구 평문 lat/lon 을 사용한다({@code GpsCoordCrypto.resolveToBigDecimal}).
 * 매퍼는 복호화하지 않는다.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — {@code selectAttdGpsTrail}의 SELECT 순서와 완전 일치 유지할 것.
 */
public record EmployeeGpsTrailRow(
      String gpsId
    , String latEnc        // 위도 암호문(AES-GCM, NULL=백필 전 구 평문 행)
    , String lonEnc        // 경도 암호문
    , BigDecimal lat       // 구 평문 위도(전환기 fallback)
    , BigDecimal lon       // 구 평문 경도
    , BigDecimal accuracy
    , String apiCallDate
    , String apiCallTime
    , String isMocked
    , String gpsInfoType
    // 위치정보 동의철회·중지 S5: 좌표 파기 사유[WITHDRAW/RETENTION]. NULL = 미파기.
    //   ★좌표가 NULL 인데 이 값도 NULL 이면 "원래 좌표가 안 잡힌 행"(기기 사정)이다.
    , String gpsPurgeReasonCd
) {
}
