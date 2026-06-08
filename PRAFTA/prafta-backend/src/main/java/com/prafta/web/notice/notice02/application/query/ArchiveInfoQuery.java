package com.prafta.web.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.ArchiveConstants;
import com.prafta.web.notice.notice02.application.param.ArchiveInfoParam;

/**
 * 자료실 단건 상세/첨부/비번해시/수정판정 공통 쿼리.
 * baimValCdForJoin 은 단건 상세의 자료타입명 LEFT JOIN 용 코드그룹 상수(첨부/비번/판정 쿼리에선 미사용).
 */
public record ArchiveInfoQuery(
    String noticeId
    , String gvCmpnyCd
    , String baimValCdForJoin
){
    public static ArchiveInfoQuery from(ArchiveInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveInfoQuery(
            param.noticeId()
            , param.gvCmpnyCd()
            , ArchiveConstants.ARCHIVE_BAIM_VAL_CD
        );
    }

    public static ArchiveInfoQuery of(String noticeId, String gvCmpnyCd) {
        return new ArchiveInfoQuery(noticeId, gvCmpnyCd, ArchiveConstants.ARCHIVE_BAIM_VAL_CD);
    }
}
