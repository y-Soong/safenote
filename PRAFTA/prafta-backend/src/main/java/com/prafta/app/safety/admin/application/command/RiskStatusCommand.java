package com.prafta.app.safety.admin.application.command;

/**
 * H5 위험성평가 상태전환 UPDATE 커맨드.
 *
 * <p>WHERE 에 현재 상태 가드(expectedCurrent)를 둬 동시 전환을 직렬화한다(낙관적 락 컬럼 부재 대체).
 *    영향 0건이면 동시 전환(이미 다른 상태) → 서비스가 409.
 *
 * <p>revalDate/revalBeforeDesc 는 002 전환 시 채워지며, 그 외 전이에서는 null 일 수 있다(매퍼에서
 *    targetStatus 분기로 컬럼 갱신 범위를 제한한다 — 002 만 REVAL_* 갱신).
 */
public record RiskStatusCommand(
      String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , String targetStatus       // 전환 목표 상태(002/003/004)
    , String expectedCurrent    // 현재 상태 가드(WHERE)
    , String revalDate          // YYYYMMDD(서비스에서 '-' 제거·검증, 002 전환만)
    , String revalBeforeDesc    // 임시조치(002 전환만)
    , String gvUserCd
) {
}
