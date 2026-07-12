package com.prafta.web.risk.riskai01.application.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LLM 이 지정 JSON 스키마로 반환하는 유해요인·개선안 도출 파싱 결과(내부 모델).
 *
 * <p>★v3.6: LLM 출력은 JSON 이 아니라 라인 프로토콜(ABSTAIN / H: / M: 줄)이다 — HCX-005 가
 *    중첩 JSON 괄호 불일치를 상습 반복해 JSON 을 폐기. RiskAi01ServiceImpl.parseDerive 가
 *    줄 단위 파싱으로 본 구조를 조립한다(파싱 불가 시 null 반환 후 재시도→자유생성 폴백).
 * <p>v3: 유해요인별 개선안 그룹핑 — hazards 각 항목이 자신의 measures 를 중첩 보유(최상위 measures 제거).
 * <p>★citations 는 LLM 출력 스키마에서 제거(HCX JSON 안정화): 서버가 markers→근거 청크 대조로
 *    citations/usedChunkIds 를 결정적으로 조립한다(RiskAi01ServiceImpl.buildCitationsFromMarkers).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeriveLlmResult(
    boolean abstained,
    List<HazardOut> hazards
) {

    /** 유해요인 1건: 서술 텍스트 + 각주 마커 배열([n]) + 해당 유해요인의 개선안 목록(v3 그룹핑). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HazardOut(
        String text,
        List<String> markers,
        List<Item> measures
    ) {}

    /** 도출 항목(개선안): 서술 텍스트 + 각주 마커 배열([n]). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        String text,
        List<String> markers
    ) {}
}
