package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 완료 상세(A10) 헤더 조회 결과.
 *
 * <p>tb_tbm_session(title/contentBody) + 본인 tb_tbm_attendance(이수상태/서명파일코드/종료일시).
 * <p>endedAt = IFNULL(S.ENDED_AT, AT.EXIT_AT) (중도퇴실 케이스 보정, Q10).
 * <p>mySignFileMgmtCd: 서명 파일코드 원본. signFilePath/signFileExt 와 함께 서비스에서
 *   FileUrlSigner 로 서명 절대 URL(mySignUrl)을 발급한다(TB_FILE_INFO LEFT JOIN).
 */
@Getter
@Setter
public class TbmCompletionResult {
    private String title;
    private String contentBody;
    private String mySignFileMgmtCd;     // EXIT_SIGN_FILE_MGMT_CD
    private String signFilePath;         // TB_FILE_INFO.FILE_PATH (서명 이미지 URL 발급용)
    private String signFileExt;          // TB_FILE_INFO.FILE_EXT
    private String completionStatusCd;   // SYS053
    private String endedAt;              // yyyy-MM-dd HH:mm
}
