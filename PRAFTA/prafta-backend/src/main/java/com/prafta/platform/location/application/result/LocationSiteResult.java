package com.prafta.platform.location.application.result;

import java.math.BigDecimal;

/**
 * 대상 회사 사업장 목록 1행(TB_SITE — 지도 중심/지오펜스 원용 좌표 포함).
 *
 * <p>사업장 좌표는 개인위치정보가 아니다(시설 좌표) — SMS 게이트 없이 응답 가능.
 * ⚠️ record 매핑은 SELECT 컬럼 순서 의존 — XML SELECT 순서와 일치 유지.
 */
public record LocationSiteResult(
    String siteCd
    , String siteNo     // 사업장번호(사용자 표시용 코드 — 사업장 검색 팝업/입력칸 표기)
    , String siteNm
    , BigDecimal lat
    , BigDecimal lon
    , String gpsRange   // varchar(m) — 빈값/NULL 이면 프론트에서 원 미표시
) {
}
