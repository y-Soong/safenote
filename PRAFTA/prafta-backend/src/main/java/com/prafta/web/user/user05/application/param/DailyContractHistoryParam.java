package com.prafta.web.user.user05.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 일일사용자 계약이력 조회 파라미터 (User_05 계약이력 팝업).
 *
 * <p>userCd 는 대상 일용직 사용자코드(리소스 키). 회사/조회자/권한은 JWT 클레임에서만 도출하며,
 * 대상의 사업장 인가는 서비스가 DB 재조회로 검증한다(파라미터 불신 — IDOR 차단).
 */
public record DailyContractHistoryParam(
    String userCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static DailyContractHistoryParam from(String userCd, TokenInfo tokenInfo) {

        if (userCd == null || userCd.isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new DailyContractHistoryParam(
            userCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
