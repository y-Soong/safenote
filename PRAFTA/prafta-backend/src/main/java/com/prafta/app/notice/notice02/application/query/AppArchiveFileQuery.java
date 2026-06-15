package com.prafta.app.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.application.param.AppArchiveFileDlParam;

/**
 * 앱 자료실 첨부 단건 조회 쿼리(다운로드 토큰 발급/노출 검증용).
 */
public record AppArchiveFileQuery(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
){
    public static AppArchiveFileQuery from(AppArchiveFileDlParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppArchiveFileQuery(
            param.noticeId()
            , param.fileMgmtCd()
            , param.gvCmpnyCd()
        );
    }
}
