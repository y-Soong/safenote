package com.prafta.app.tbm.tbm01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-tbm-A10: 완료 상세 응답.
 *
 * <p>materialTitles = 자료 묶음 제목만, riskTitles = 위험성 displayName 목록(제목만).
 * <p>mySignFileMgmtCd: 본인 종료 서명 파일코드(EXIT_SIGN_FILE_MGMT_CD).
 * <p>mySignUrl: 서명 이미지 서명 절대 URL(FileUrlSigner). 서명 파일 없으면 NULL — 프론트 graceful 처리.
 * <p>endedAt = IFNULL(세션 ENDED_AT, 내 EXIT_AT) (Q10).
 */
@Getter
@Builder
public class TbmCompletionResponse {
    private final String title;
    private final String contentBody;
    private final List<String> materialTitles;
    private final List<String> riskTitles;
    private final String mySignFileMgmtCd;
    private final String mySignUrl;
    private final String completionStatusCd;   // SYS053
    private final String endedAt;
}
