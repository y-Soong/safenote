package com.prafta.app.ai.ai01.client;

import java.util.List;

/**
 * HCX 멀티턴 대화 1턴(role + 텍스트 + 선택 이미지).
 *
 * <p>PRAFTA-WEB_003 v2(대화형 이미지분석): {@link LlmAnswerClient#chat(String, List)} 입력 단위.
 *    HCX 는 stateless 이므로 매 호출마다 전 대화를 messages 배열로 재구성한다.
 *    이미지 요청당5/턴당1 제약상 이미지는 통상 "첫 user 턴"에만 싣되(서비스가 재부착),
 *    본 record 는 어느 턴에든 이미지를 실을 수 있도록 {@code images} 를 보유한다.
 *
 * <ul>
 *   <li>{@code role} — "user" 또는 "assistant"(system 은 {@code chat} 의 별도 인자로 투입).</li>
 *   <li>{@code text} — 해당 턴의 발화 텍스트.</li>
 *   <li>{@code images} — 이미지 파트 목록(없으면 null 또는 빈 리스트).</li>
 * </ul>
 */
public record HcxTurn(String role, String text, List<ImagePart> images) {}
