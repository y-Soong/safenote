package com.prafta.web.notice.notice01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeDeleteParam;

/**
 * 공지 논리삭제(DEL_YN='Y') 커맨드.
 */
public record NoticeDeleteCommand(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeDeleteCommand from(NoticeDeleteParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeDeleteCommand(
            param.noticeId()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
