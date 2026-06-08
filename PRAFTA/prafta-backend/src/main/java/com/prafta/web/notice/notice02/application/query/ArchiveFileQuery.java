package com.prafta.web.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.param.ArchiveFileDlParam;

/**
 * 자료실 첨부 단건 조회 쿼리(다운로드 토큰 발급/스트림용).
 */
public record ArchiveFileQuery(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
){
    public static ArchiveFileQuery from(ArchiveFileDlParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveFileQuery(
            param.noticeId()
            , param.fileMgmtCd()
            , param.gvCmpnyCd()
        );
    }

    /** 다운로드 토큰 검증 후 스트림용(토큰 claim 으로 직접 구성). */
    public static ArchiveFileQuery of(String cmpnyCd, String noticeId, String fileMgmtCd) {
        return new ArchiveFileQuery(noticeId, fileMgmtCd, cmpnyCd);
    }
}
