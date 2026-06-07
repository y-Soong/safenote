package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R5 교육자료 리스트(탭3) 조회 파라미터.
 *
 * <p>식별자(cmpnyCd/userCd/siteCd/authCd)는 JWT 클레임에서만 도출한다(IDOR 차단).
 */
public record AdminEduMaterialListParam(
    String mtrlType
    , String title
    , String useYn
    , int page
    , int pageSize
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminEduMaterialListParam of(String mtrlType, String title, String useYn,
            Integer page, Integer pageSize, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        return new AdminEduMaterialListParam(
            mtrlType
            , title
            , useYn
            , p
            , ps
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
