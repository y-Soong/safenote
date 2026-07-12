package com.prafta.web.risk.riskimpr01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementItemSaveParam;

/**
 * 개선항목 INSERT/UPDATE 커맨드.
 * fileMgmtCd 는 서비스에서 사진 저장 후 주입(사진 미변경 시 null → 매퍼에서 보존 처리).
 * improvementSeq 는 신규 시 서비스에서 채번하여 주입한다.
 */
public record ImprovementItemCommand(
    String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , Integer improvementSeq
    , String improveDate
    , String improveDesc
    , String fileMgmtCd
    , Integer likelihoodScore
    , Integer severityScore
    , String riskLv
    , String gvUserCd
){
    public static ImprovementItemCommand from(ImprovementItemSaveParam param, Integer improvementSeq, String fileMgmtCd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ImprovementItemCommand(
            param.gvCmpnyCd()
            , param.siteCd()
            , param.processCd()
            , param.assessmentCd()
            , improvementSeq
            , param.improveDate()
            , param.improveDesc()
            , fileMgmtCd
            , param.likelihoodScore()
            , param.severityScore()
            , param.riskLv()
            , param.gvUserCd()
        );
    }
}
