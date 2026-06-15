package com.prafta.app.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 앱 자료타입 드롭다운 조회 파라미터(header JWT 만). cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 */
public record AppArchiveTypeParam(
    String gvCmpnyCd
){
    public static AppArchiveTypeParam from(TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppArchiveTypeParam(tokenInfo.gv_cmpnyCd());
    }
}
