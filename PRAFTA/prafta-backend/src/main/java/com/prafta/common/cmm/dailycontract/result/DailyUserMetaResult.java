package com.prafta.common.cmm.dailycontract.result;

/**
 * 일용직 사용자 메타(사업장/이름) — 서명 게이트 판정·서명 저장의 서버측 단일 출처.
 *
 * <p>사업장은 JWT 클레임이 아니라 TB_DAILY_USER.SITE_CD 를 신뢰한다(클레임 누락/조작 대비).
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record DailyUserMetaResult(
    String siteCd
    , String userNm
) {
}
