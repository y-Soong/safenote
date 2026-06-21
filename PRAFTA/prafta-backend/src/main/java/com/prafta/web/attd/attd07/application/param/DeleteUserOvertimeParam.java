package com.prafta.web.attd.attd07.application.param;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.DeleteUserOvertimeRequest;

/**
 * Server-internal carrier for POST /attd07/delete-user-overtime (com-016-E).
 *
 * 검증된 요청 body 와 JWT 파생 신원(cmpny / user / authCd / siteCd)을 결합한다.
 * 매니저 게이트(canManageNode)와 cross-site IDOR 가드를 위해 gvAuthCd / gvSiteCd 를 함께 운반한다.
 *
 * <p>com-013 #6 의 단건(otId)을 com-016-E 에서 다건(otIds)으로 확장했다.
 */
public record DeleteUserOvertimeParam(
      List<String> otIds
    , String siteCd
    , String userCd
    , String workYmd
    , String nodeCd
    , String reqReason

    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserOvertimeParam.class);

    public static DeleteUserOvertimeParam from(DeleteUserOvertimeRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("DeleteUserOvertimeParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("DeleteUserOvertimeParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getOtIds() == null || request.getOtIds().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - required field missing: otIds");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        // 개별 otId 가 빈 문자열/NULL 이면 거부(부분 누락 방지).
        for (String otId : request.getOtIds()) {
            if (otId == null || otId.isEmpty()) {
                log.warn("DeleteUserOvertimeParam.from - blank otId in otIds");
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - required field missing: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getUserCd() == null || request.getUserCd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - required field missing: userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getWorkYmd() == null || request.getWorkYmd().isEmpty()) {
            log.warn("DeleteUserOvertimeParam.from - required field missing: workYmd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // cross-site IDOR guard - body siteCd 가 호출자의 JWT site scope 와 다르면 거부한다.
        if (!request.getSiteCd().equals(tokenInfo.gv_siteCd())) {
            log.warn("OT delete request site mismatch with token. requested={}, token={}",
                    request.getSiteCd(), tokenInfo.gv_siteCd());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new DeleteUserOvertimeParam(
              List.copyOf(request.getOtIds())
            , request.getSiteCd()
            , request.getUserCd()
            , request.getWorkYmd()
            , request.getNodeCd()
            , request.getReqReason()

            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
