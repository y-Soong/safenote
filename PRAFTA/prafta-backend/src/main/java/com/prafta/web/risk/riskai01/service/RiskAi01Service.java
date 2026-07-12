package com.prafta.web.risk.riskai01.service;

import com.prafta.web.risk.riskai01.application.param.RiskAiParam;
import com.prafta.web.risk.riskai01.dto.response.RiskAiDerivationResponse;

/**
 * 위험성평가 AI 유해요인·개선안 도출 서비스(PRAFTA-WEB_003 v3 절차 기반).
 *
 * <p>기능: 기존 결과/대화 로드(getDerivation), 이미지 확정 루프 채팅(chatImage — kickoff/재질의),
 *    이미지 이해 확정(confirmImage, LLM 미호출), 유해요인·개선안 도출(derive, RAG+LLM, 그룹핑).
 *    ★캡(횟수 제한) 없음.
 */
public interface RiskAi01Service {

    // 패널 오픈 시 기존 도출 결과 + 대화 이력 + 확정 여부 로드
    RiskAiDerivationResponse getDerivation(RiskAiParam param);

    // 관리자 보완 설명(직접입력) 저장(blur 자동저장). AI 실행과 무관하게 단발 저장/반환(v2.1 — v3 관리자 의견 재활용)
    RiskAiDerivationResponse saveSupplement(RiskAiParam param);

    // 이미지 확정 루프 채팅: kickoff(자동 첫 질의) 또는 관리자 정정 메시지(+추가 이미지) → VLM 확인 질의 → 이력 갱신/반환
    RiskAiDerivationResponse chatImage(RiskAiParam param);

    // 이미지 이해 확정(Yes): "예, 맞습니다." 이력 append + IMG_CONFIRMED='Y' 저장(LLM 미호출)(v3)
    RiskAiDerivationResponse confirmImage(RiskAiParam param);

    // RAG 그라운딩 + LLM 으로 유해요인별 개선안 그룹 도출 → JSON 저장(v3)
    RiskAiDerivationResponse derive(RiskAiParam param);

    // 초기화(처음부터 다시): 도출 행 DELETE(대화이력/도출결과/보완설명 전체 삭제, LLM 미호출) → 초기 상태 반환
    RiskAiDerivationResponse reset(RiskAiParam param);

    // 미저장 정리(commit-on-save): SAVED_YN='N' 행 삭제 후 남은(확정) 행 기준 상태 반환(LLM 미호출).
    // 팝업 오픈/닫기 시 미확정 작업분 정리용
    RiskAiDerivationResponse discardUnsaved(RiskAiParam param);

    // 저장 확정(commit-on-save): 평가 저장 성공 시 SAVED_YN='Y' 확정 후 현재 행 기준 상태 반환(LLM 미호출)
    RiskAiDerivationResponse commit(RiskAiParam param);
}
