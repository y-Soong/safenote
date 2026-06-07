package com.prafta.web.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeSaveParam;

/**
 * 공지 채번 쿼리(회사별: N + YYYYMMDD + 3자리 SEQ).
 * near_miss 채번 패턴(MAX+1 self-numbering)을 미러하되 사업장 무관(회사 단위) 스코프.
 */
public record NoticeIdSeqQuery(
    String gvCmpnyCd
){
    public static NoticeIdSeqQuery from(NoticeSaveParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeIdSeqQuery(param.gvCmpnyCd());
    }
}
