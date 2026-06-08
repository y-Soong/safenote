package com.prafta.web.notice.notice02.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.param.ArchiveDeleteParam;

/**
 * 자료실 논리삭제(DEL_YN='Y') 커맨드.
 */
public record ArchiveDeleteCommand(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
){
    public static ArchiveDeleteCommand from(ArchiveDeleteParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveDeleteCommand(
            param.noticeId()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
