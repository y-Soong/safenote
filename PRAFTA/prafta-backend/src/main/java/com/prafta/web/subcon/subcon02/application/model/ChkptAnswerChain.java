package com.prafta.web.subcon.subcon02.application.model;

import java.util.List;

/**
 * 기점 점검대상 기준의 체인 스냅샷(기점 제외한 전 티어) — qa M-3.
 *
 * <p>점검대상 매핑(LINK_SRC)은 저장 1회 동안 불변이므로, 문항 루프 밖에서 <b>1회만</b> 해석해 재사용한다.
 * 과거에는 문항마다 (부모 1 + 자식 N) 링크 조회가 반복되어 저장 1회당 N+1 이 발생했다.
 */
public record ChkptAnswerChain(
    List<ChkptChainTier> tiers
){

    public boolean isEmpty() {
        return tiers == null || tiers.isEmpty();
    }
}
