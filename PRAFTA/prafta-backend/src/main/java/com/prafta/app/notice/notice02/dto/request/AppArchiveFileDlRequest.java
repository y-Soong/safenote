package com.prafta.app.notice.notice02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 앱 자료실 첨부 다운로드 토큰 발급 요청. cmpnyCd/userCd 는 JWT 에서만 도출(IDOR 차단).
 */
@Getter
@Setter
@NoArgsConstructor
public class AppArchiveFileDlRequest {
    private String noticeId;
    private String fileMgmtCd;
}
