package com.prafta.web.notice.notice01.result;

/**
 * 공지 첨부 1건 결과 VO (tb_notice_file + tb_file_info join).
 * filePath 는 정적 서빙 상대경로. 실제 다운로드는 단기 토큰 발급 후 file-download EP 사용.
 */
public record NoticeFileResult(
    String fileMgmtCd
    , String fileNm
    , String fileExt
    , String filePath
    , Integer sortIdx
){
}
