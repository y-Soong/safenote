package com.prafta.app.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.dto.request.AppArchiveFileDlRequest;

/**
 * 앱 자료실 첨부 다운로드 토큰 발급 파라미터.
 * cmpnyCd/userCd 는 JWT 에서만 도출(IDOR 차단). 자료실=회사 전체 공통이므로 노출 대상 재귀 검증은
 * 불필요(존재+CMPNY+ARCHIVE+DEL_YN='N' 확인으로 충분).
 */
public record AppArchiveFileDlParam(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AppArchiveFileDlParam from(AppArchiveFileDlRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppArchiveFileDlParam(
            request.getNoticeId()
            , request.getFileMgmtCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
