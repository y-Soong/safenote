package com.prafta.web.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.dto.request.NoticeFileDlRequest;

/**
 * 공지 첨부 다운로드 토큰 발급 파라미터.
 * cmpnyCd/userCd 는 JWT 에서만 도출(IDOR 차단).
 */
public record NoticeFileDlParam(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String curSiteCd
    , String curNodeCd
){
    public static NoticeFileDlParam from(NoticeFileDlRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // 노출 대상 재검증(047-001)을 위해 현재 소속(사업장/노드)·권한을 JWT 에서만 도출(IDOR 차단).
        return new NoticeFileDlParam(
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
