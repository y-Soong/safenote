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
    /**
     * 소정-07 - 근로자 명시 청구 확인 값('Y' 만 확인으로 인정). 단축 기간(육아기·가족돌봄)에만 소비된다.
     * 미전송(null)이면 확인 없음으로 보고 단축 기간 한정 거부(ATTD_400_201) — fail-safe.
     */
    , String reducedWorkOtClaimYn
) {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserOvertimeRequestParam.class);

    /**
     * 소정-07 이전 형태(12-인자) 호환 생성자.
     *
     * <p>앱 관리자 승인 경로({@code AppAdminApprovalServiceImpl.doApproveAsis})처럼 근로자 명시 청구
     * 확인 값을 운반하지 않는 기존 호출부를 그대로 유지하기 위한 위임 생성자다. 확인 값은 null 로
     * 채워지므로 단축 기간(육아기·가족돌봄) 대상이면 거부된다(fail-safe 방향 — 소정-07 확정).
     */
    public UpdateUserOvertimeRequestParam(String userCd, String siteCd, String nodeCd, String workYmd,
                                          String attdId, String reqId, String reqReason,
                                          List<OvertimeItemModel> overtimes,
                                          String gvCmpnyCd, String gvUserCd, String gvAuthCd, String gvSiteCd) {
        this(userCd, siteCd, nodeCd, workYmd, attdId, reqId, reqReason, overtimes,
                gvCmpnyCd, gvUserCd, gvAuthCd, gvSiteCd, null);
    }

    /** 소정-07 - 근로자 명시 청구가 확인되었는지 여부('Y' 만 인정). */
    public boolean workerClaimConfirmed() {
        return "Y".equalsIgnoreCase(reducedWorkOtClaimYn);
    }

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

        // SEC-017 - cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관
        //   (사업장 권한 원장 TB_USER_SITE_AUTH 기반 인가. 대상 사용자 스코프는 서비스의
        //    selectUserExistInScope 재검증으로 유지).

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
            // 소정-07: 근로자 명시 청구 확인 값(단축 기간에만 소비. 미전송이면 null → 거부).
            //   앱 OvertimeParam 과 동일하게 trim 한다 — " Y" 같은 공백 포함 전송이 미확인으로
            //   오판정되지 않게 하기 위함(security Info 지적, 2026-08-12).
            , trimToNull(request.getReducedWorkOtClaimYn())
        );
    }

    /** 앞뒤 공백을 제거하고, 빈 문자열이면 null 로 반환한다(앱 {@code OvertimeParam.trim} 과 동일 규약). */
    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
