package com.prafta.web.subcon.subcon03.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 공유 요청 생성 후보 조회 파라미터(PRAFTA-SUBCON-T3 §5-2).
 *
 * <p>prvCmpnyCd 가 없으면 상대 회사 목록만, 있으면 그 회사와 사업장 체인이 있는
 * <b>내 사업장</b> 목록까지 반환한다(제공사 사업장 목록은 절대 노출하지 않는다).
 */
public record ShareReqCandidatesParam(
    String prvCmpnyCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ShareReqCandidatesParam from(String prvCmpnyCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ShareReqCandidatesParam(
            prvCmpnyCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
