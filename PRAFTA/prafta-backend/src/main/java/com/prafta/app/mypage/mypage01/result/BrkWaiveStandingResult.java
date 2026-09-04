package com.prafta.app.mypage.mypage01.result;

/**
 * BW-12(§7-1, 2026-09-04): 휴게 미이용 상시 요청 현행값 + 노출 조건 판정 재료(TB_USER 단건).
 *
 * <p>⚠️ MyBatis 위치 기반 매핑 — record 필드 순서 = SELECT 컬럼 순서. 컬럼 추가는 반드시 맨 끝.
 */
public record BrkWaiveStandingResult(
      /** 현행값 'Y'/'N' (DDL DEFAULT 'N' — NULL 방어는 서비스에서). */
      String standingYn

    /** 최근 변경 시각(서버 포맷 'yyyy-MM-dd HH:mm', 미변경이면 null). */
    , String standingDtime

    /** 고용형태 [SYS041] REGULAR/DAILY 등 — DAILY 거부·정규직 노출 조건 판정용. */
    , String employmentType
) {
}
