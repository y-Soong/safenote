package com.prafta.app.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice01.application.param.AppNoticePopupParam;

/**
 * 앱 로그인 팝업 / 내 공지 목록 / 미열람 카운트 판정 쿼리.
 * isDaily 는 서비스에서 tb_daily_user 매칭으로 산출해 주입(§6-2).
 */
public record AppNoticePopupQuery(
    String gvCmpnyCd
    , String gvUserCd
    , String curSiteCd
    , String curNodeCd
    , boolean isDaily
){
    public static AppNoticePopupQuery from(AppNoticePopupParam param, boolean isDaily) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AppNoticePopupQuery(
            param.gvCmpnyCd()
            , param.gvUserCd()
            , param.curSiteCd()
            , param.curNodeCd()
            , isDaily
        );
    }
}
