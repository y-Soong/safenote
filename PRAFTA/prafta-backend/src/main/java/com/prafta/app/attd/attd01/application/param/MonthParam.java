package com.prafta.app.attd.attd01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.attd.attd01.dto.request.MonthRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-002: 이번달 조회 Param.
 *
 * <p>IDOR 가드: CMPNY_CD/USER_CD/SITE_CD 는 JWT 출처. 클라이언트는 yearMonth(YYYYMM) 만 지정.
 */
public record MonthParam(
    String cmpnyCd
    , String userCd
    , String siteCd
    , String yearMonth
    , TokenInfo tokenInfo
) {
    public static MonthParam from(MonthRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String yearMonth = request.getYearMonth();
        if (yearMonth == null || !yearMonth.matches("\\d{6}")) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        String siteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(siteCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new MonthParam(cmpnyCd, userCd, siteCd, yearMonth, tokenInfo);
    }
}
