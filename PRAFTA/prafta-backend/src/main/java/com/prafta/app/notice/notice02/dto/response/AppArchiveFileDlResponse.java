package com.prafta.app.notice.notice02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 자료실 첨부 다운로드 단기 토큰 응답.
 * token: JWT scope=ARCHIVE_FILE_DL (만료 5분). expiresInSec: 만료 초. fileNm: 다운로드 파일명(표시용).
 * 실제 스트림은 웹 @NoAuth /webApi/notice02/file-download?token= 재사용.
 */
@Getter
@Builder
public class AppArchiveFileDlResponse {
    private String token;
    private long expiresInSec;
    private String fileNm;
}
