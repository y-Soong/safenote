package com.prafta.web.tbm.tbmai01.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TBM 세부항목 AI 대화 1턴(응답 + AI_CHAT_JSON 저장 스키마 공용).
 *
 * <p>{@code role} 은 "user"(관리자 보완 지시) 또는 "assistant"(AI 초안·응답), {@code text} 는 발화 텍스트.
 *    ★이미지·PDF 바이트는 저장/전송하지 않는다(chat 호출 때마다 디스크에서 재로드해 재부착).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TbmAiChatTurn(
    String role
    , String text
) {}
