package com.prafta.web.attd.attd07.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.AttdCloseRequest;

/**
 * 근태 마감/해제 실행 Param (prafta-019-C).
 */
public record AttdCloseParam(
      String siteCd
    , String nodeCd
    , String incSubNodeYn
    , String closeYm
    , String closeDesc
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
) {
    public static AttdCloseParam from(AttdCloseRequest request, TokenInfo tokenInfo) {
        if (request == null || tokenInfo == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isBlank()
                || request.getCloseYm() == null || request.getCloseYm().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()
                || tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // cross-site IDOR 가드 — body siteCd가 JWT gv_siteCd와 다르면 거부 (형제 SEC-017/019 동일 패턴)
        if (!request.getSiteCd().equals(tokenInfo.gv_siteCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        return new AttdCloseParam(
              request.getSiteCd()
            , request.getNodeCd()
            , request.getIncSubNodeYn()
            , request.getCloseYm()
            , request.getCloseDesc()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
        );
    }
}
