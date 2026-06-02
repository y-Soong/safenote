package com.prafta.app.leave.leave01.application.query;

import com.prafta.app.leave.leave01.application.param.MyLeaveSummaryParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-005: 연차 현황 조회 Query (mapper 진입).
 * <p>기준 오늘(todayYmd, YYYYMMDD)은 DB NOW() 기준이 원칙이나, 그룹 집계/사용예정/소멸임박
 * 쿼리가 동일 기준일을 공유해야 하므로 서비스에서 DB 의 todayYmd 를 1회 조회하여 주입한다
 * (쿼리 간 자정 경계 불일치 방지 — home01 selectTodayYmd 패턴 동일).
 * <p>grantType prefix 는 그룹(STATUTORY/MANUAL) 집계 SQL 의 LIKE 패턴이며,
 * TOTAL 은 prefix 미주입(null) 으로 prefix 무관 전체합을 산출한다.
 */
public record MyLeaveSummaryQuery(
    String cmpnyCd
    , String userCd
    , String todayYmd
    , String grantTypePrefix
) {
    public static MyLeaveSummaryQuery from(MyLeaveSummaryParam param, String todayYmd) {

        if (param == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new MyLeaveSummaryQuery(
            param.cmpnyCd()
            , param.userCd()
            , todayYmd
            , null
        );
    }

    /**
     * 그룹 집계용 변형 Query 생성(동일 cmpnyCd/userCd/todayYmd + grantType prefix 주입).
     * <p>prefix 가 null 이면 TOTAL(prefix 무관 전체합) 집계.
     */
    public MyLeaveSummaryQuery withGrantTypePrefix(String prefix) {
        return new MyLeaveSummaryQuery(this.cmpnyCd, this.userCd, this.todayYmd, prefix);
    }
}
