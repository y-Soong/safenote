package com.prafta.web.risk.riskai01.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 이미지분석 대화 1턴(응답 + IMG_CHAT_JSON 저장 스키마 공용).
 *
 * <p>{@code role} 은 "user" 또는 "assistant", {@code text} 는 발화 텍스트.
 *    ★이미지 바이트는 저장/전송하지 않는다(chat 호출 때마다 디스크에서 재로드해 재부착).
 * <p>{@code hidden} 은 v3 kickoff(자동 첫 질의)의 숨김 user 턴 표시. FE 는 hidden 턴을 렌더하지 않는다.
 *    구버전 JSON 행(hidden 미존재)은 null → false 취급(하위호환).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatTurn(
    String role
    , String text
    , Boolean hidden
) {}
