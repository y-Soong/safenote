package com.prafta.app.notice.notice02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.app.notice.notice02.dto.request.AppArchiveListRequest;

/**
 * 앱 자료실 목록 조회 파라미터. cmpnyCd 는 JWT 에서만 도출(IDOR 차단).
 * registMonth('YYYY-MM'/'YYYYMM')는 쿼리 변환 단계(AppArchiveListQuery)에서 startDate/endDate 로 변환한다.
 */
public record AppArchiveListParam(
    String archiveTypeCd
    , String registMonth
    , String titleKeyword
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static AppArchiveListParam from(AppArchiveListRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AppArchiveListParam(
            request.getArchiveTypeCd()
            , request.getRegistMonth()
            , request.getTitleKeyword()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
