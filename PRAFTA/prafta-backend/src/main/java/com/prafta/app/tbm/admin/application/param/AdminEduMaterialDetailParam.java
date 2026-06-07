package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * R5 교육자료 상세/삭제 파라미터. mtrlCd 는 path 에서 받되 토큰 스코프로 소유 재검증(IDOR).
 */
public record AdminEduMaterialDetailParam(
    String mtrlCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminEduMaterialDetailParam of(String mtrlCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminEduMaterialDetailParam(
            mtrlCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
