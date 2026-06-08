package com.prafta.app.notice.notice01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 앱 로그인 팝업/내 공지 목록/미열람 카운트 판정 파라미터(헤더 JWT 만 사용).
 * 수신자 현재 소속(사업장/노드)은 JWT 클레임 기준(발행시점 스냅샷 미사용, 웹 NoticePopupParam 미러).
 * isDaily(일용직 여부)는 서버에서 tb_daily_user 매칭으로 판정한다.
 */
public record AppNoticePopupParam(
    String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String curSiteCd
    , String curNodeCd
){
    public static AppNoticePopupParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppNoticePopupParam(
            tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_nodeCd()
        );
    }
}
