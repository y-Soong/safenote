package com.prafta.app.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice01.application.param.AppNoticeInfoParam;

/**
 * 앱 단건 상세/첨부 조회 공통 쿼리.
 * gvUserCd 는 상세 조회 시 사용자별 뱃지(isUnread) 계산용.
 */
public record AppNoticeInfoQuery(
    String noticeId
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AppNoticeInfoQuery from(AppNoticeInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppNoticeInfoQuery(
            param.noticeId()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }

    /** 첨부 list 조회 등 noticeId 직접 구성용. */
    public static AppNoticeInfoQuery of(String noticeId, String cmpnyCd, String userCd) {
        return new AppNoticeInfoQuery(noticeId, cmpnyCd, userCd);
    }
}
