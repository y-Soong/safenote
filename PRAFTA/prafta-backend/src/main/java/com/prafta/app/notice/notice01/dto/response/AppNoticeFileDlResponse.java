package com.prafta.app.notice.notice01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 공지 첨부 다운로드 단기 토큰 응답.
 * token: JWT scope=NOTICE_FILE_DL (만료 5분). expiresInSec: 만료 초.
 * fileNm: 다운로드 파일명(클라 표시용).
 * 실제 스트림은 웹 @NoAuth /webApi/notice01/file-download?token= 재사용.
 */
@Getter
@Builder
public class AppNoticeFileDlResponse {
    private String token;
    private long expiresInSec;
    private String fileNm;
}
