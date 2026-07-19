package com.prafta.web.subcon.subcon02.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.dto.request.ChkptLinkRequest;

/**
 * 순회점검 구성 연동 실행/해제 파라미터(PRAFTA-SUBCON-T6-02).
 *
 * <p>행위자 소속 회사(gvCmpnyCd)/행위자(gvUserCd)/역할(gvAuthCd)은 JWT 클레임에서만 도출한다.
 * 당사자 조건(실행=SRC 소속만, 해제=양측)은 매퍼 조건부 UPDATE 로 강제한다(0행=404 존재 비노출).
 */
public record ChkptLinkParam(
    Long linkId
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
){
    public static ChkptLinkParam from(ChkptLinkRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new ChkptLinkParam(
            request.getLinkId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
        );
    }
}
