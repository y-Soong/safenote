package com.prafta.web.attd.attd08.application.param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd08.dto.request.AttdGpsTrailRequest;

/**
 * Attd_08 GPS 궤적 조회 파라미터.
 *
 * <p>★ security H-1(2026-08-07): 응답이 <b>복호화된 위·경도 평문</b>이라 인가가 필수다.
 * 파라미터는 {@code attdId} 뿐이라 인가를 여기서 판정할 수 없으므로, 서비스가 근태 행의
 * 사업장/부서를 먼저 조회한 뒤 {@code assertSiteAccess} + {@code canManageNode} 로 판정한다.
 * 그 입력이 되는 토큰 클레임을 함께 싣는다.
 */
public record AttdGpsTrailParam(
      String attdId
    , String gvCmpnyCd
    , String gvAuthCd
    , String gvUserCd
    , String gvSiteCd
) {

    private static final Logger log = LoggerFactory.getLogger(AttdGpsTrailParam.class);

    public static AttdGpsTrailParam from(AttdGpsTrailRequest request, TokenInfo tokenInfo) {

        if (request == null) {
            log.warn("AttdGpsTrailParam.from - request is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (tokenInfo == null) {
            log.warn("AttdGpsTrailParam.from - tokenInfo is null");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        if (request.getAttdId() == null || request.getAttdId().isBlank()) {
            log.warn("AttdGpsTrailParam.from - required field missing: attdId");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // ATTD_ID is varchar(20) on the DB side but the application format is a
        // numeric sequence (yyyyMMdd + seq). Reject non-numeric input early so
        // we never bind arbitrary strings to downstream queries.
        // [PRAFTA-010-1-020] attdId 가 비숫자인 경우는 "필수 누락"이 아니라
        // "유효하지 않은 값"이므로 COMMON_400_002 로 응답한다.
        // (위 blank 검증의 COMMON_400_001 = 진짜 필수 누락과 구분한다.)
        try {
            Long.parseLong(request.getAttdId());
        } catch (NumberFormatException e) {
            log.warn("AttdGpsTrailParam.from - attdId 형식 오류(비숫자). attdId={}",
                    request.getAttdId());
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        // security H-1: 인가 판정 입력(토큰 클레임) 결손이면 fail-closed.
        if (tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isEmpty()
                || tokenInfo.gv_siteCd() == null || tokenInfo.gv_siteCd().isEmpty()) {
            log.warn("AttdGpsTrailParam.from - token claim missing: gv_cmpnyCd / gv_siteCd");
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new AttdGpsTrailParam(
              request.getAttdId()
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_authCd()
            , tokenInfo.gv_userCd()
            , tokenInfo.gv_siteCd()
        );
    }
}
