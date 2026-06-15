package com.prafta.app.notice.notice02.result;

/**
 * 앱 자료실 첨부 1건 결과 VO (tb_notice_file + tb_file_info join).
 * filePath 는 정적 서빙 상대경로. 실제 다운로드는 단기 토큰 발급 후 웹 file-download 스트림 재사용.
 *
 * <p>⚠️ MyBatis record 매핑: SELECT 컬럼 순서 = 생성자 인자 순서(위치 기반).
 */
public record AppArchiveFileResult(
    String fileMgmtCd
    , String fileNm
    , String fileExt
    , String filePath
    , Integer sortIdx
){
}
