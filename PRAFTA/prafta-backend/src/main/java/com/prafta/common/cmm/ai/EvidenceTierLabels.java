package com.prafta.common.cmm.ai;

import java.util.Map;

/**
 * 근거 층위(evidence tier) 코드 → 화면 표시 문구 매핑(prafta-062, 2026-09-02 사용자 확정 D9 후보 A).
 *
 * <p>층위는 코퍼스 출처 단위 속성(tb_ai_corpus_source.evidence_tier)이며, 표시 문구는
 *    FE 무로직 원칙에 따라 서버가 완성해 내려준다(FE 는 보간만).
 * <p>미지정(null/공백)·미등록 코드는 {@code null} 을 반환한다 — 화면은 배지를 표시하지 않는다(종전 동작).
 */
public final class EvidenceTierLabels {

    /** 코드 → 표시 문구. 값 목록은 파이프라인 화이트리스트(00_validate_registry.VALID_TIER)와 동일 유지. */
    private static final Map<String, String> LABELS = Map.of(
        "LAW", "법적 의무",
        "GUIDE", "권고 지침",
        "STAT", "고위험 통계",
        "CASE", "유사 재해",
        "REF", "참고 자료");

    private EvidenceTierLabels() {
        // 상수 클래스 — 인스턴스화 금지
    }

    /**
     * 층위 코드의 표시 문구를 반환한다.
     *
     * @param tier 층위 코드(LAW|GUIDE|STAT|CASE|REF). null/공백/미등록 코드 허용.
     * @return 표시 문구. 미지정·미등록이면 null(배지 미표시).
     */
    public static String labelOf(String tier) {
        if (tier == null || tier.isBlank()) {
            return null;
        }
        return LABELS.get(tier.trim());
    }
}
