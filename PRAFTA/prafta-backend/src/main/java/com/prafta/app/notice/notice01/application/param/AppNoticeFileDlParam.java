package com.prafta.app.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice01.dto.request.AppNoticeFileDlRequest;

/**
 * 앱 공지 첨부 다운로드 토큰 발급 파라미터.
 * cmpnyCd/userCd 는 JWT 에서만 도출(IDOR 차단). 노출 대상 재검증용으로 현재 소속(사업장/노드)도 JWT 도출.
 */
public record AppNoticeFileDlParam(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String curSiteCd
    , String curNodeCd
){
    public static AppNoticeFileDlParam from(AppNoticeFileDlRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppNoticeFileDlParam(
            request.getNoticeId()
            , request.getFileMgmtCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_nodeCd()
        );
    }
}
