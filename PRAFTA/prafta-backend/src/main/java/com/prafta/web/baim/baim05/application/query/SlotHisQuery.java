package com.prafta.web.baim.baim05.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.baim.baim05.application.param.SlotHisParam;

/**
 * PRAFTA-055-3 — 슬롯 사용 이력 조회 쿼리. cutoffYmd(yyyyMMdd) 기준일 이후 점유분만 조회(최근 30일).
 */
public record SlotHisQuery(
    String siteCd
    , String slotNo
    , String cutoffYmd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static SlotHisQuery from(SlotHisParam param, String cutoffYmd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new SlotHisQuery(
            param.siteCd()
            , param.slotNo()
            , cutoffYmd
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
