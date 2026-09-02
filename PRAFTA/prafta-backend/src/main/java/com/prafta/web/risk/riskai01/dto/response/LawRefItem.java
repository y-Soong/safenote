package com.prafta.web.risk.riskai01.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 유해요인 1건에 사후 매핑된 법령 조문 1건(prafta-062 배포 D — 062-07).
 *
 * <p>★정책서 §4.5(정확성 사유 verbatim): 조문은 LLM 프롬프트/컨텍스트에 절대 투입하지 않는다.
 *    서버가 법령 전용 검색(searchLawTrack) 결과의 DB 원문을 무변경 패스스루로 첨부만 하며,
 *    {@code content} 는 코퍼스 청크 원문 그대로다(가공·요약·절단 금지 — 오독 유발).
 * <p>{@code effectiveDateText} 는 표시용 문자열(예: "2026.03.02")로 서버가 완성한다(FE 무로직 원칙).
 *    {@code articleLabel} 예: "제32조의2(보호구의 지급 등)".
 * <p>{@code DerivedHazardGroup.lawRefs} 에 중첩되어 HAZARD_JSON(네이티브 json 타입)에 함께 저장된다 —
 *    도출 시점 조문 스냅샷 보존(D8=(a)). 구 저장분은 누락 필드 null 파싱으로 하위호환(마이그레이션 불요).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LawRefItem(
    String lawName
    , String articleLabel
    , String content
    , String sourceLocator
    , String effectiveDate
    , String effectiveDateText
    , String sourceUrl
    , String evidenceTier
    , String evidenceTierLabel
) {}
