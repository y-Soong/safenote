package com.prafta.app.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 자료실 첨부 1건 요청(다건 리스트 요소).
 * 파일 본문은 별도 upload-file API 로 tb_file_info 에 선저장된 FILE_MGMT_CD 를 매핑한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AppArchiveFileRequest {
    private String fileMgmtCd;
    private Integer sortIdx;
}
