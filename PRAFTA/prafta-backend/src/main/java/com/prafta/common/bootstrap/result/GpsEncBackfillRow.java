package com.prafta.common.bootstrap.result;

import java.math.BigDecimal;

/**
 * GPS좌표-암호화-전환-05: 백필 대상 행(3테이블 공용 형태).
 *
 * <p>rowKey 는 테이블별 식별 컬럼(TB_USER_ATTD_GPS=GPS_ID / TB_TBM_ATTENDANCE=ATTENDANCE_CD /
 * TB_TBM_SESSION=SESSION_CD). UPDATE 는 (CMPNY_CD, rowKey) 로 수행한다.
 *
 * <p>⚠️ record 매핑은 SELECT 컬럼 순서 의존 — 각 select 의 컬럼 순서와 완전 일치 유지.
 * 좌표 평문(lat/lon)은 암호화 입력으로만 사용하고 로그/예외 메시지에 출력하지 않는다.
 */
public record GpsEncBackfillRow(
      String cmpnyCd
    , String rowKey
    , BigDecimal lat
    , BigDecimal lon
) {
}
