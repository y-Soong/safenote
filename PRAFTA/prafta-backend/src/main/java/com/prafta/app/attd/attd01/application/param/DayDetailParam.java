package com.prafta.app.attd.attd01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.app.attd.attd01.dto.request.DayDetailRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-002: 일자 상세 조회 Param.
 *
 * <p>IDOR 가드: CMPNY_CD/USER_CD/SITE_CD 는 JWT 출처. 클라이언트는 workYmd(YYYYMMDD) 만 지정한다.
 *   workYmd 형식 검증(8자리 숫자) 후 서비스로 전달한다.
 */
public record DayDetailParam(
    String cmpnyCd
    , String userCd
    , String siteCd
    , String siteNm
    , String workYmd
    , TokenInfo tokenInfo
) {
    public static DayDetailParam from(DayDetailRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String workYmd = request.getWorkYmd();
        if (!isYmd8(workYmd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_002);
        }

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        String siteCd = tokenInfo.gv_siteCd();
        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(siteCd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new DayDetailParam(
            cmpnyCd
            , userCd
            , siteCd
            , tokenInfo.gv_siteNm()
            , workYmd
            , tokenInfo
        );
    }

    private static boolean isYmd8(String s) {
        return s != null && s.matches("\\d{8}");
    }
}
