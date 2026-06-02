package com.prafta.app.attd.attd01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.attd.attd01.dto.request.WeekRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-002: 이번주 조회 Param.
 *
 * <p>IDOR 가드: CMPNY_CD/USER_CD/SITE_CD 는 JWT 출처. 클라이언트는 weekStartYmd(YYYYMMDD) 만 지정.
 */
public record WeekParam(
    String cmpnyCd
    , String userCd
    , String siteCd
    , String weekStartYmd
    , TokenInfo tokenInfo
) {
    public static WeekParam from(WeekRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String weekStartYmd = request.getWeekStartYmd();
        if (weekStartYmd == null || !weekStartYmd.matches("\\d{8}")) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        String siteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(siteCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new WeekParam(cmpnyCd, userCd, siteCd, weekStartYmd, tokenInfo);
    }
}
