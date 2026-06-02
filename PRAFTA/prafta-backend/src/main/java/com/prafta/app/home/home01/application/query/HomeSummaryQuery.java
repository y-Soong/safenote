package com.prafta.app.home.home01.application.query;

import com.prafta.app.home.home01.application.param.HomeSummaryParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-001: 메인화면 요약 조회 Query (mapper 진입).
 * <p>오늘 일자(todayYmd, YYYYMMDD)는 DB NOW()/CURDATE() 기준이 원칙이나,
 * 4개 영역 쿼리가 동일 기준일을 공유해야 하므로 서비스에서 DB 의 CURDATE() 값을
 * 1회 조회하여 주입한다(쿼리 간 자정 경계 불일치 방지).
 */
public record HomeSummaryQuery(
    String cmpnyCd
    , String siteCd
    , String userCd
    , String todayYmd
) {
    public static HomeSummaryQuery from(HomeSummaryParam param, String todayYmd) {

        if (param == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new HomeSummaryQuery(
            param.cmpnyCd()
            , param.siteCd()
            , param.userCd()
            , todayYmd
        );
    }
}
