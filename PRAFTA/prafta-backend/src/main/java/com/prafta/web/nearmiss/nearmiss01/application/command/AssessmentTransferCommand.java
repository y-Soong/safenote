package com.prafta.web.nearmiss.nearmiss01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.nearmiss.nearmiss01.application.param.ReclassifyParam;

/**
 * 재분류(E6) 시 원 tb_risk_assessment 건의 진행상태를 '005(아차사고로 이관)'로 전환하는 커맨드.
 * 삭제/USE_YN 변경이 아니라 상태값 전환으로 추적을 보존한다(D2 확정).
 */
public record AssessmentTransferCommand(
    String siteCd
    , String srcProcessCd
    , String srcAssessmentCd
    , String gvCmpnyCd
    , String gvUserCd
){
    public static AssessmentTransferCommand from(ReclassifyParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new AssessmentTransferCommand(
            param.siteCd()
            , param.srcProcessCd()
            , param.srcAssessmentCd()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
