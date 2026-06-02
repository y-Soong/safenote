package com.prafta.app.nearmiss.nearmiss01.application.command;

import com.prafta.app.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A6 1차 확인 상태전환 Command (100->200 검토중 또는 900 반려).
 *
 * <p>200 전환 시 adminTempActionDesc 를 ADMIN_TEMP_ACTION_DESC 에 기록(보고자 IMMEDIATE_ACTION_DESC 불변).
 *    900 반려 시 rejectReason 을 REJECT_REASON 에 기록. 검토자(REVIEWER_ID)/검토일시 함께 기록.
 */
public record ChangeStatusCommand(
    String siteCd
    , String nearMissId
    , String reportStatusCd
    , String adminTempActionDesc
    , String rejectReason
    , String gvCmpnyCd
    , String gvUserCd
){
    public static ChangeStatusCommand from(ChangeStatusParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ChangeStatusCommand(
            param.siteCd()
            , param.nearMissId()
            , param.reportStatusCd()
            , param.adminTempActionDesc()
            , param.rejectReason()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
