package com.prafta.web.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.ArchiveConstants;
import com.prafta.web.notice.notice02.application.param.ArchiveListParam;

public record ArchiveListQuery(
    String archiveTypeCd
    , String titleKeyword
    , String startDate
    , String endDate
    , String gvCmpnyCd
    , String baimValCdForJoin  // 자료타입명 LEFT JOIN 용 코드그룹 상수(YJ 확정 전엔 빈값 → 타입명 NULL)
){
    public static ArchiveListQuery from(ArchiveListParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveListQuery(
            param.archiveTypeCd()
            , param.titleKeyword()
            , param.startDate()
            , param.endDate()
            , param.gvCmpnyCd()
            , ArchiveConstants.ARCHIVE_BAIM_VAL_CD
        );
    }
}
