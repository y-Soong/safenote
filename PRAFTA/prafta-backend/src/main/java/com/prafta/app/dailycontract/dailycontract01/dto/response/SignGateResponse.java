package com.prafta.app.dailycontract.dailycontract01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 계약서 서명 게이트 판정 응답 (GET /appApi/dailycontract01/sign-gate).
 *
 * <p>signRequiredYn='N' 이면 게이트 스킵(활성 계약서 없음/이미 서명/일용직 아님 — R2).
 * 게이트 조회 실패 시 프론트는 가용성 우선 통과(termsGate 관례).
 */
@Value
@Builder
public class SignGateResponse {
    String signRequiredYn;   // 'Y' | 'N'
    Integer contractVer;     // 서명 필요 시 대상 버전(스킵이면 null)
    String contractNm;       // 서명 필요 시 계약서명(스킵이면 null)
}
