package com.prafta.common.cmm.dailycontract.result;

/**
 * 현재 승인 사이클 행 (TB_DAILY_ENTRY_REQUEST — 승인시점 버전확정 T2/§4).
 *
 * <p>서명 게이트/열람/합성이 "어느 계약서 버전을 대상으로 삼는가"의 단일 출처다.
 * 선택 규칙: 소진('05') 최신 1건 우선, 없으면 <b>당일</b> 승인('02') 최신 1건
 * (선택 순서를 반전하면 아직 시작되지 않은 미래 사이클의 pin 으로 현재 세션을 판정하게 된다).
 *
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 *
 * @param reqId       승인요청ID(서명 레코드 REQ_ID 와 대조되는 사이클 식별자)
 * @param reqStatus   [SYS082] '05' 소진 / '02' 승인 (운영 진단용 — 판정에는 사용하지 않는다)
 * @param contractVer 승인 시점 확정 계약서 버전. <b>반드시 {@code Integer}</b> —
 *                    {@code int} 로 두면 NULL 이 0 으로 매핑되어 "배포 전 레거시(NULL)"가
 *                    "승인 시점 미등록(0)"으로 오판정되고, 기존 서명자가 게이트를 통과해 버린다(R5).
 *                    NULL=레거시(활성 폴백, K8) / 0=미등록(게이트 스킵, K4) / >0=확정 버전
 */
public record EntryCycleResult(
    String reqId
    , String reqStatus
    , Integer contractVer
) {
}
