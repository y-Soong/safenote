package com.prafta.app.nearmiss.nearmiss01.application.command;

import com.prafta.app.nearmiss.nearmiss01.application.param.ReportParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * A1 보고 등록 INSERT Command.
 *
 * <p>고정값(서비스/매퍼에서 강제): REPORT_STATUS_CD='100'(접수), REPORTER_ID=gvUserCd,
 *    SITE_CD=gvSiteCd (앱 직접보고). fileMgmtCd 는 사진 첨부 시에만 채워진다.
 */
public record InsertReportCommand(
    String siteCd
    , String nearMissId
    , String incidentTypeCd
    , String processCd
    , String occurDtime
    , String locationDesc
    , String description
    , String potentialSeverityCd
    , String immediateActionDesc
    , String fileMgmtCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static InsertReportCommand from(ReportParam param, String nearMissId, String fileMgmtCd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new InsertReportCommand(
            param.tokenInfo().gv_siteCd()
            , nearMissId
            , param.incidentTypeCd()
            , param.processCd()
            , param.occurDtime()
            , param.locationDesc()
            , param.description()
            , param.potentialSeverityCd()
            , param.immediateActionDesc()
            , fileMgmtCd
            , param.tokenInfo().gv_cmpnyCd()
            , param.tokenInfo().gv_userCd()
        );
    }
}
