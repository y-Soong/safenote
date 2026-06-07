package com.prafta.web.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeFileDlParam;

/**
 * 공지 첨부 단건 조회 쿼리(다운로드 토큰 발급/스트림용).
 */
public record NoticeFileQuery(
    String noticeId
    , String fileMgmtCd
    , String gvCmpnyCd
){
    public static NoticeFileQuery from(NoticeFileDlParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeFileQuery(
            param.noticeId()
            , param.fileMgmtCd()
            , param.gvCmpnyCd()
        );
    }

    /** 다운로드 토큰 검증 후 스트림용(토큰 claim 으로 직접 구성). */
    public static NoticeFileQuery of(String cmpnyCd, String noticeId, String fileMgmtCd) {
        return new NoticeFileQuery(noticeId, fileMgmtCd, cmpnyCd);
    }
}
