package com.prafta.web.notice.notice01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 공지 첨부 단건 업로드 응답.
 * 발급된 FILE_MGMT_CD 와 원본 파일명을 반환하여 프론트가 첨부 목록을 구성하고
 * 최종 save-notice 요청의 fileList 에 fileMgmtCd 를 매핑하도록 한다.
 */
@Getter
@Builder
public class NoticeFileUploadResponse {
    private String fileMgmtCd;
    private String fileNm;
}
