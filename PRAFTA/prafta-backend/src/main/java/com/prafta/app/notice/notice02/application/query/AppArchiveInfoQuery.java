package com.prafta.app.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.AppArchiveConstants;
import com.prafta.app.notice.notice02.application.param.AppArchiveInfoParam;

/**
 * 앱 자료실 단건 상세/첨부 공통 쿼리.
 * baimValCdForJoin 은 단건 상세의 자료타입명 LEFT JOIN 용 코드그룹 상수(첨부 쿼리에선 미사용).
 */
public record AppArchiveInfoQuery(
    String noticeId
    , String gvCmpnyCd
    , String baimValCdForJoin
){
    public static AppArchiveInfoQuery from(AppArchiveInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppArchiveInfoQuery(
            param.noticeId()
            , param.gvCmpnyCd()
            , AppArchiveConstants.ARCHIVE_BAIM_VAL_CD
        );
    }
}
