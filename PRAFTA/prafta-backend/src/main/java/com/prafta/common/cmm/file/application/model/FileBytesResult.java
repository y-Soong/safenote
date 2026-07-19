package com.prafta.common.cmm.file.application.model;

/**
 * 저장 파일 원본 바이트 + 메타(범용 — 이미지 한정 아님).
 *
 * <p>PRAFTA-SUBCON-T7(Q4 전 파일타입 복제): {@link ImageBytesResult} 는 이미지 화이트리스트 전용이라
 *    PDF 등 비이미지 첨부를 조용히 누락시킨다. 위험성평가/아차사고 첨부는 유형 무관 전부 복제해야 하므로
 *    확장자를 보존하는 범용 로더({@code FileService.loadFileBytes})의 결과로 이 record 를 사용한다.
 *
 * <p>{@code fileExt} 는 점 제외 소문자(예: "pdf", "jpg"). 복제 시 파일명 조립에 그대로 쓴다.
 */
public record FileBytesResult(
    byte[] data
    , String contentType
    , String fileExt
    , String fileNm
) {}
