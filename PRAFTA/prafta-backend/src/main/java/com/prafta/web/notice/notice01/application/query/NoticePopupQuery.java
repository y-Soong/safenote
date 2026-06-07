package com.prafta.web.notice.notice01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.notice.notice01.application.param.NoticePopupParam;

/**
 * 로그인 팝업 노출 판정 쿼리.
 * isDaily 는 서비스에서 tb_daily_user 매칭으로 산출해 주입(요청서 §6-2).
 */
public record NoticePopupQuery(
    String gvCmpnyCd
    , String gvUserCd
    , String curSiteCd
    , String curNodeCd
    , boolean isDaily
){
    public static NoticePopupQuery from(NoticePopupParam param, boolean isDaily) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new NoticePopupQuery(
            param.gvCmpnyCd()
            , param.gvUserCd()
            , param.curSiteCd()
            , param.curNodeCd()
            , isDaily
        );
    }
}
