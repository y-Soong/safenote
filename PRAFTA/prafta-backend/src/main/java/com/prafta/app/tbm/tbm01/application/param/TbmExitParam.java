package com.prafta.app.tbm.tbm01.application.param;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.tbm.tbm01.dto.request.TbmExitRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C2: 종료 Param.
 * <p>multipart 요청(sessionCd/exitPwd) + 종료 서명 파일 + tokenInfo 를 service 단일 인자로 정리한다.
 * <p>D1 정책: 종료 서명 파일은 필수 — null/empty 면 거부한다.
 * <p>USER_CD/CMPNY_CD 는 token 출처만 사용한다(IDOR 차단).
 */
public record TbmExitParam(
    String sessionCd
    , String exitPwd
    , MultipartFile signFile
    , Integer appForegroundSec
    , TokenInfo tokenInfo
) {
    public static TbmExitParam from(TbmExitRequest request, MultipartFile signFile, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        if (!StringUtils.hasText(request.getSessionCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!StringUtils.hasText(request.getExitPwd()))
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (!StringUtils.hasText(tokenInfo.gv_cmpnyCd()) || !StringUtils.hasText(tokenInfo.gv_userCd()))
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // prafta-051-08: 포그라운드 누적초는 nullable 보조지표 — 검증 없이 그대로 전달(방어는 service 에서).
        return new TbmExitParam(
            request.getSessionCd()
            , request.getExitPwd()
            , signFile
            , request.getAppForegroundSec()
            , tokenInfo
        );
    }
}
