package com.prafta.web.attd.attd07.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.dto.request.DailyAttdDetailsRequest;

/**
 * /attd07/daily-attd-details 의 서버 내부 운반체.
 *
 * <p>요청 body의 식별자와 JWT 기반 회사/사용자/사이트/권한 클레임을 함께 전달한다.
 * 서비스 계층은 {@link TokenInfo}를 직접 다루지 않는다.
 *
 * <p>SEC-019:
 *   - 본 endpoint는 일자 상세(타인의 출퇴근/OT/PII 포함)를 조회하므로 cross-site/cross-user
 *     IDOR 가드가 필요하다. {@code from} 단계에서 body의 {@code siteCd}가 JWT
 *     {@code gv_siteCd}와 일치하는지 우선 검증하고, 서비스 계층에서 대상 사용자의
 *     scope를 추가로 재검증한다.
 *   - 매니저 전용 화면(AttdDayDetailPop / Attd_07 등)에서만 호출되므로 {@code gvAuthCd}를
 *     운반하여 서비스 계층이 {@code AuthRoleUtils.isManager}로 게이트한다.
 */
public record DailyAttdDetailsParam(
      String attdId
    , String siteCd
    , String userCd
    , String userId
    , String workYmd
    , String nodeCd
    , String gvCmpnyCd
    , String gvUserCd
    , String gvSiteCd
    , String gvAuthCd
) {

    private static final Logger log = LoggerFactory.getLogger(DailyAttdDetailsParam.class);

    public static DailyAttdDetailsParam from(DailyAttdDetailsRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("DailyAttdDetailsParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("DailyAttdDetailsParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing claim: gv_cmpnyCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing claim: gv_userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing claim: gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo.gv_authCd() == null || tokenInfo.gv_authCd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing claim: gv_authCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getSiteCd() == null || request.getSiteCd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing field: siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getUserCd() == null || request.getUserCd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing field: userCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (request.getWorkYmd() == null || request.getWorkYmd().isEmpty()) {
            log.warn("DailyAttdDetailsParam.from - missing field: workYmd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // SEC-019 - cross-site IDOR 가드는 서비스 계층 SiteAccessService.assertSiteAccess 로 이관.
        //   (토큰 사업장 등식 강제는 User_03 사업장 권한 원장(TB_USER_SITE_AUTH)과 불일치 —
        //    하도급 미러 사업장 등 다사업장 관리가 막혔다. 서비스의 selectUserExistInCmpnySite
        //    재검증과 함께 다층 방어는 유지된다.)

        return new DailyAttdDetailsParam(
              request.getAttdId()
            , request.getSiteCd()
            , request.getUserCd()
            , request.getUserId()
            , request.getWorkYmd()
            , request.getNodeCd()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
            , tokenInfo.gv_authCd()
        );
    }
}
