package com.prafta.app.tbm.tbm01.result;

import lombok.Getter;
import lombok.Setter;

/**
 * prafta-app-tbm: 완료 상세(A10) 헤더 조회 결과.
 *
 * <p>tb_tbm_session(title/contentBody) + 본인 tb_tbm_attendance(이수상태/서명파일코드/종료일시).
 * <p>endedAt = IFNULL(S.ENDED_AT, AT.EXIT_AT) (중도퇴실 케이스 보정, Q10).
 * <p>mySignFileMgmtCd: 파일코드 원본. 백엔드에 파일코드→이미지 URL 변환 컨트롤러 부재로
 *   URL 대신 파일코드만 응답한다(web tbm04 와 동일 규약, Q6 플래그).
 */
@Getter
@Setter
public class TbmCompletionResult {
    private String title;
    private String contentBody;
    private String mySignFileMgmtCd;     // EXIT_SIGN_FILE_MGMT_CD (URL 변환 미지원)
    private String completionStatusCd;   // SYS053
    private String endedAt;              // yyyy-MM-dd HH:mm
}
