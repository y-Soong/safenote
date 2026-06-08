package com.prafta.web.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice02.application.param.ArchiveSaveParam;

/**
 * 자료실 채번 쿼리(회사별: 'A' + YYYYMMDD + 3자리 SEQ, NOTICE_TYPE='ARCHIVE' 스코프).
 * 공지('N' prefix)와 ID 공간을 분리한다.
 */
public record ArchiveIdSeqQuery(
    String gvCmpnyCd
){
    public static ArchiveIdSeqQuery from(ArchiveSaveParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ArchiveIdSeqQuery(param.gvCmpnyCd());
    }
}
