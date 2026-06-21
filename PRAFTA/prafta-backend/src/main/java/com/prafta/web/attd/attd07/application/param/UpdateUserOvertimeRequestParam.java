package com.prafta.web.attd.attd07.application.param;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.application.model.OvertimeItemModel;
import com.prafta.web.attd.attd07.dto.request.OvertimeItemRequest;
import com.prafta.web.attd.attd07.dto.request.UpdateUserOvertimeRequestRequest;

/**
 * Server-internal carrier for /attd07/update-user-overtime-requests.
 *
 * Combines the validated request body with JWT-derived (cmpny / user / site /
 * authCd) identity so the service layer never has to touch the raw
 * {@link TokenInfo}.
 *
 * For SEC-012 compliance the {@code from} factory only logs the internal field
 * name on the server side and throws a generic {@code COMMON_400_001}; the
 * client never sees the precise missing field name.
 *
 * SEC-015 / SEC-017:
 *   - {@code gvAuthCd} is carried so the service can enforce the manager-only
 *     gate on this endpoint.
 *   - {@code gvSiteCd} is carried so the service can compare the body's
 *     requested SITE_CD against the JWT scope (cross-site IDOR guard).
 */
public record UpdateUserOvertimeRequestParam(
      String userCd
    , String siteCd
    , String nodeCd
    , String workYmd
    , String attdId
    , String reqId
    , String reqReason
    , List<OvertimeItemModel> overtimes
    , String gvCmpnyCd
    , String gvUserCd
    , String gvAuthCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserOvertimeRequestParam.class);

    public static UpdateUserOvertimeRequestParam from(UpdateUserOvertimeRequestRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("UpdateUserOvertimeRequestParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("UpdateUserOvertimeRequestParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getUserCd() == null || request.getUserCd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing field: userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing field: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getWorkYmd() == null || request.getWorkYmd().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - missing field: workYmd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getOvertimes() == null || request.getOvertimes().isEmpty()) {
            log.warn("UpdateUserOvertimeRequestParam.from - overtimes list is empty");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // SEC-017 - cross-site IDOR guard.
        // Reject the request if the body asks to operate on a site different
        // from the caller's JWT site scope. The service layer additionally
        // re-checks the target user's site via selectUserExistInScope.
        if (!request.getSiteCd().equals(tokenInfo.gv_siteCd())) {
            log.warn("OT request site mismatch with token. requested={}, token={}",
                    request.getSiteCd(), tokenInfo.gv_siteCd());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        List<OvertimeItemModel> models = new ArrayList<>(request.getOvertimes().size());
        for (OvertimeItemRequest ot : request.getOvertimes()) {
            if (ot == null) {
                log.warn("UpdateUserOvertimeRequestParam.from - null element inside overtimes list");
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            models.add(new OvertimeItemModel(
                  ot.getOtId()
                , ot.getStartDate()
                , ot.getStartTime()
                , ot.getEndDate()
                , ot.getEndTime()
            ));
        }

        return new UpdateUserOvertimeRequestParam(
              request.getUserCd()
            , request.getSiteCd()
            , request.getNodeCd()
            , request.getWorkYmd()
            , request.getAttdId()
            , request.getReqId()
            , request.getReqReason()
            , models
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
