package com.prafta.app.ai.ai01.client;

/**
 * LLM vision(멀티모달) 입력 이미지 1건.
 *
 * <p>{@code base64} 는 원본 이미지 바이트의 base64 인코딩(데이터 URL 접두어 없이 순수 base64 — HCX dataUri.data 규격),
 *    {@code mediaType} 는 {@code image/jpeg} · {@code image/png} · {@code image/webp} 중 하나다
 *    (svg 는 스크립트 삽입 위험으로 금지 — 파일 read 단계 화이트리스트에서 이미 차단).
 *    HCX dataUri 는 media_type 이 불요하므로 {@code mediaType} 은 전송에 쓰지 않고 상위 검증 용도로만 유지된다.
 */
public record ImagePart(String base64, String mediaType) {}
