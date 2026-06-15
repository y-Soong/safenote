package com.prafta.app.notice.notice02.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.application.param.AppArchiveSaveParam;

/**
 * 앱 자료실 채번 쿼리('A' + YYYYMMDD + 3자리 SEQ, NOTICE_TYPE='ARCHIVE' 스코프).
 * 공지('N' prefix)와 ID 공간을 분리한다(웹 자료실과 동일 ID 공간 — 같은 tb_notice 공유).
 */
public record AppArchiveIdSeqQuery(
    String gvCmpnyCd
){
    public static AppArchiveIdSeqQuery from(AppArchiveSaveParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppArchiveIdSeqQuery(param.gvCmpnyCd());
    }
}
