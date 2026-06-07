package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R6 이력 리스트(탭4) 조회 파라미터.
 *
 * <p>statusCd 미지정 시 COMPLETED/CANCELLED 전체. startDate/endDate 는 프론트 native YYYY-MM-DD 그대로.
 * 식별자는 JWT 클레임에서만 도출(IDOR 차단).
 */
public record AdminHistoryListParam(
    String statusCd
    , String startDate
    , String endDate
    , String keyword
    , int page
    , int pageSize
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminHistoryListParam of(String statusCd, String startDate, String endDate,
            String keyword, Integer page, Integer pageSize, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        return new AdminHistoryListParam(
            statusCd
            , startDate
            , endDate
            , keyword
            , p
            , ps
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
