package com.prafta.common.cmm.file.application.model;

/**
 * 서버측에서 로드한 이미지 원본 바이트 + media_type.
 *
 * <p>PRAFTA-WEB_003: Claude vision 전송용. {@code mediaType} 은 image/jpeg·png·webp 중 하나
 *    (확장자 화이트리스트에서 매핑). 디스크에 파일이 없으면 서비스는 {@code null} 을 반환한다.
 */
public record ImageBytesResult(
    byte[] data
    , String mediaType
) {}
