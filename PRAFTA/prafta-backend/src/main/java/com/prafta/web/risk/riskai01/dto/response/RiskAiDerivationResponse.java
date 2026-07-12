package com.prafta.web.risk.riskai01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 위험성평가 AI 도출 응답(GET derivation / POST chat-image / POST confirm-image / POST derive 공통 전체 상태).
 *
 * <ul>
 *   <li>{@code chatTurns} — 이미지분석 대화 이력([{role,text,hidden}], v2 멀티턴 + v3 hidden).</li>
 *   <li>{@code suppDesc} — 관리자 보완 설명(직접입력, 재열람 로드용, v2.1 — v3 "관리자 의견"으로 재활용).</li>
 *   <li>{@code imgConfirmed} — 이미지 이해 확정 여부(v3 — 재열람 시 단계 복원).</li>
 *   <li>{@code hazards} — 도출 유해요인 그룹(v3 — 유해요인별 개선안 measures 중첩, 각주 markers 포함).</li>
 *   <li>{@code citations} — 근거 출처(서버 검증 통과분).</li>
 *   <li>{@code verbatimRefs} — verbatim 참고 원문(LLM 미경유 서버 패스스루, 원문 무변경 — 그라운딩 개선 C).</li>
 *   <li>{@code abstained} — 코퍼스 근거부재 자유생성(각주 없음) 여부.</li>
 *   <li>{@code disclaimer} — 참고용 고지(최종 판단은 담당자).</li>
 * </ul>
 */
@Getter
@Builder
public class RiskAiDerivationResponse {

    private List<ChatTurn> chatTurns;
    private String suppDesc;
    private boolean imgConfirmed;
    private List<DerivedHazardGroup> hazards;
    private List<CitationItem> citations;
    private List<VerbatimRefItem> verbatimRefs;
    private boolean abstained;
    private String disclaimer;
}
