package com.prafta.app.tbm.admin.application.param;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * tbm04-manager-sign: 주관자(개설자) 서명 동반 상태 전이/사후서명 파라미터.
 *
 * <p>교육 종료(endSession — 서명 필수 편입)와 종료 세션 사후서명(signCompletedSession) 이 공용한다.
 * signFile 은 multipart 파트 'item'(참석자 exit 계약 미러). 서명 필수 검증(null/empty/파일 검증)은
 * 서비스에서 수행한다. 식별자(회사/사용자/사업장/권한)는 JWT 클레임에서만 도출한다(IDOR 차단).
 */
public record AdminManagerSignParam(
    String sessionCd
    , MultipartFile signFile
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
){
    public static AdminManagerSignParam of(String sessionCd, MultipartFile signFile, TokenInfo tokenInfo) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        return new AdminManagerSignParam(
            sessionCd
            , signFile
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
