package com.prafta.web.risk.riskai01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 위험성평가 AI 도출 공통 요청(GET derivation / POST chat-image / POST confirm-image / POST derive).
 *
 * <p>평가건 4축 중 CMPNY_CD 는 JWT 에서만 도출하고, 나머지 3축(SITE_CD/PROCESS_CD/ASSESSMENT_CD)만 바디로 받는다.
 *    (@ModelAttribute · @RequestBody 양쪽 바인딩을 위해 getter/setter 를 둔다.)
 * <p>{@code userMessage} 는 chat-image(이미지분석 채팅) 전송 메시지. derive/derivation 에서는 미사용.
 * <p>{@code suppDesc} 는 관리자 보완 설명(직접입력, 선택). chat-image/derive/supplement 에서 AI 입력 보조로 투입·저장한다.
 * <p>{@code kickoff} 는 v3 이미지 확정 루프의 자동 첫 질의 플래그(chat-image 전용). true 면 userMessage 불요.
 * <p>{@code adminImages} 는 v3 관리자 추가 이미지(요청단발, 서버 미저장 — 해당 턴에만 부착).
 *    캡: 최대 2장, 장당 디코드 3MB, jpg/png/webp — 위반 시 AI_400_004.
 */
@Getter
@Setter
public class RiskAiRequest {
    private String siteCd;
    private String processCd;
    private String assessmentCd;
    private String userMessage;
    private String suppDesc;
    private Boolean kickoff;
    private List<AdminImageIn> adminImages;

    /** 관리자 추가 이미지 1건(base64 = 순수 base64, mediaType = image/jpeg·png·webp). */
    @Getter
    @Setter
    public static class AdminImageIn {
        private String base64;
        private String mediaType;
    }
}
