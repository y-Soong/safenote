package com.prafta.app.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice01.application.param.AppNoticeFileDlParam;

/**
 * 앱 공지 첨부 단건 조회 쿼리(다운로드 토큰 발급용).
 */
public record AppNoticeFileQuery(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
){
    public static AppNoticeFileQuery from(AppNoticeFileDlParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppNoticeFileQuery(
            param.noticeId()
            , param.fileMgmtCd()
            , param.gvCmpnyCd()
        );
    }
}
