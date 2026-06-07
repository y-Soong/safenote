package com.prafta.web.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticeInfoParam;

/**
 * 단건 상세/첨부/대상 조회 공통 쿼리.
 * gvUserCd 는 상세 조회 시 사용자별 뱃지(isUnread/isUpdated) 계산용.
 */
public record NoticeInfoQuery(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
){
    public static NoticeInfoQuery from(NoticeInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticeInfoQuery(
            param.noticeId()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
