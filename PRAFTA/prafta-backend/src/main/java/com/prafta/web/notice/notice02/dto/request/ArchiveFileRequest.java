package com.prafta.web.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자료실 첨부 1건 요청(다건 리스트 요소).
 * 파일 업로드는 별도 upload-file API 로 tb_file_info 에 선저장된 FILE_MGMT_CD 를 매핑한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ArchiveFileRequest {
    private String fileMgmtCd;
    private Integer sortIdx;
}
