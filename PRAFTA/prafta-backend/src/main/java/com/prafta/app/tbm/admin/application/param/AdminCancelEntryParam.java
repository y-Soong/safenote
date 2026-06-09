package com.prafta.app.tbm.admin.application.param;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-051 R-C(E13) 입실취소(GPS 이탈자 내보내기) 파라미터.
 *
 * <p>D-3 확정: 교육 미시작(OPENED) 단계의 단순 입실취소(강제퇴실/미이수 아님). 재입실 자유.
 * sessionCd/attendanceCd 는 path, 식별자는 JWT 클레임에서만 도출(IDOR 차단). 요청 바디/사유 없음.
 */
public record AdminCancelEntryParam(
    String sessionCd
    , String attendanceCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminCancelEntryParam of(String sessionCd, String attendanceCd, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminCancelEntryParam(
            sessionCd
            , attendanceCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
