package com.prafta.web.tbm.tbmai01.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.tbm.tbmai01.dto.request.TbmAiRequest;

/**
 * TBM 세부항목 AI 분석·확정 파라미터.
 *
 * <p>식별자(cmpnyCd/userCd/authCd)는 JWT 클레임에서만 도출한다(IDOR 차단). 자료/항목 스코프 키
 *    (mtrlCd/mtrlItemCd)는 바디에서 받아 서비스에서 회사 스코프 조인으로 소유·존재를 검증한다.
 */
public record TbmAiParam(
    String mtrlCd
    , String mtrlItemCd
    , String userMessage
    , String confirmText
    , String adminNote
    , String manualText
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {
    public static TbmAiParam from(TbmAiRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new TbmAiParam(
            request.getMtrlCd()
            , request.getMtrlItemCd()
            , request.getUserMessage()
            , request.getConfirmText()
            , request.getAdminNote()
            , request.getManualText()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
