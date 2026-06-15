package com.prafta.app.notice.notice02.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 자료실 첨부 단건 업로드 응답.
 * 발급된 FILE_MGMT_CD 와 원본 파일명을 반환하여 프론트가 save-archive 의 fileList 에 매핑한다.
 */
@Getter
@Builder
public class AppArchiveFileUploadResponse {
    private String fileMgmtCd;
    private String fileNm;
}
