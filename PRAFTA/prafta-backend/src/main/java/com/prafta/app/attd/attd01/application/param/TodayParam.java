package com.prafta.app.attd.attd01.application.param;

import org.springframework.util.StringUtils;

import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-002: 오늘 근태 조회 Param.
 *
 * <p>IDOR 가드: CMPNY_CD/USER_CD/SITE_CD 는 모두 JWT(tokenInfo) 출처. 파라미터로 USER_CD 를 받지 않는다.
 *   workYmd 는 서버 today(YYYYMMDD) 를 사용한다.
 */
public record TodayParam(
    String cmpnyCd
    , String userCd
    , String siteCd
    , String siteNm
    , String workYmd
    , TokenInfo tokenInfo
) {
    public static TodayParam from(TokenInfo tokenInfo, String todayYmd) {

        if (tokenInfo == null)
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String cmpnyCd = tokenInfo.gv_cmpnyCd();
        String userCd = tokenInfo.gv_userCd();
        String siteCd = tokenInfo.gv_siteCd();

        if (!StringUtils.hasText(cmpnyCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(siteCd)) {
            // 사업장 미선택/토큰 손상 시 명확한 에러.
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }

        return new TodayParam(
            cmpnyCd
            , userCd
            , siteCd
            , tokenInfo.gv_siteNm()
            , todayYmd
            , tokenInfo
        );
    }
}
