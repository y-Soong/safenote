package com.prafta.app.risk.risk01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.risk.risk01.dto.request.RiskTypeInfoRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B2: 위험성평가 구분/분류/발생상황 조회 Param.
 * <p>request + tokenInfo 를 합쳐 service 진입 인자로 단일화한다.
 * <p>prafta-036-C(H-2): siteCd 는 tokenInfo.gv_siteCd() 로 강제 캐노니컬라이즈 (cross-site IDOR 차단).
 *   request.siteCd 필드는 FE 호환을 위해 수신은 하되 무시한다.
 *   (TB_RISK_TYPE / TB_RISK_SITE_HAZARD 사업장 필터 조건은 token 출처 siteCd 로 사용)
 */
public record RiskTypeInfoParam(
    String siteCd
    , String gvCmpnyCd
    , String gvUserCd
    , TokenInfo tokenInfo
) {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(RiskTypeInfoParam.class);

    public static RiskTypeInfoParam from(RiskTypeInfoRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        // prafta-036-C(H-2): siteCd 토큰 캐노니컬라이즈
        String tokenSiteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(tokenSiteCd)) {
            // FE 가 사업장 선택 없이 호출한 경우 명확한 에러
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String reqSiteCd = request.getSiteCd();
        if (StringUtils.hasText(reqSiteCd) && !tokenSiteCd.equals(reqSiteCd)) {
            // 클라이언트가 다른 siteCd 를 보낸 경우 경고만(action: 토큰 값으로 강제)
            log.warn("[risk01] siteCd 캐노니컬라이즈: 요청={}, 토큰={} -> 토큰값 사용 (userCd={})",
                    reqSiteCd, tokenSiteCd, tokenInfo.gv_userCd());
        }

        return new RiskTypeInfoParam(
            tokenSiteCd
            , tokenInfo.gv_cmpnyCd()
            , tokenInfo.gv_userCd()
            , tokenInfo
        );
    }
}
