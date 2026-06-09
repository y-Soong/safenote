package com.prafta.app.tbm.admin.application.param;

import com.prafta.app.tbm.admin.dto.request.AdminQrScanRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * E11 일용직 QR 입실 파라미터(prafta-051 R-D).
 *
 * <p>sessionCd 는 path, qrPayload 는 바디(QR raw 문자열)에서 받되 식별자(회사/사용자/사업장/권한)는 JWT
 * 클레임에서만 도출한다(IDOR 차단). QR 페이로드 파싱/일용직 식별키(userCd) 추출/유효성 재검증은 서비스가 수행한다.
 */
public record AdminQrScanParam(
    String sessionCd
    , String qrPayload
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminQrScanParam from(
            String sessionCd, AdminQrScanRequest request, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String qrPayload = request != null ? request.getQrPayload() : null;

        return new AdminQrScanParam(
            sessionCd
            , qrPayload
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
