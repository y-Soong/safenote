package com.prafta.web.risk.riskai01.application.model;

/**
 * tb_risk_ai_derivation 1행(대화이력 + 도출 결과 JSON). 행이 없으면 매퍼가 null 반환(초기 상태).
 *
 * <p>⚠️ MyBatis record 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서
 *    (IMG_CHAT_JSON, SUPP_DESC, IMG_CONFIRMED, HAZARD_JSON, MEASURE_JSON, CITATION_JSON,
 *     VERBATIM_JSON, ABSTAINED).
 *    JSON 컬럼은 문자열로 수신하여 서비스에서 Jackson 파싱한다.
 */
public record RiskAiDerivationRow(
    String imgChatJson      // 이미지분석 대화이력 [{role,text,hidden}] (v2 멀티턴 + v3 hidden)
    , String suppDesc       // 관리자 보완 설명(직접입력, AI 입력 보조) (v2.1)
    , String imgConfirmed   // 이미지 이해 확정 여부 'Y' / null (v3)
    , String hazardJson     // v3: 그룹 구조 [{text,markers[],measures:[{text,markers[]}]}]
    , String measureJson    // v3 미사용 잔존(도출 시 NULL 클리어)
    , String citationJson
    , String verbatimJson   // verbatim 참고 원문 [{sourceName,dataReliability,content,hazardText,measureText}] (그라운딩 개선 C)
    , String abstained      // 'Y' / 'N' / null
) {}
