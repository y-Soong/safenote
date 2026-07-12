package com.prafta.web.risk.riskimpr01.application.command;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.risk.riskimpr01.application.param.ImprovementCompleteParam;

/**
 * 개선완료(005→003 전이) 커맨드.
 * tb_risk_assessment 의 ASSESSMENT_STATUS='003' + REVAL_* 동기화에 사용.
 */
public record ImprovementCompleteCommand(
    String gvCmpnyCd
    , String siteCd
    , String processCd
    , String assessmentCd
    , Integer revalLikelihoodScore
    , Integer revalSeverityScore
    , String revalRiskLv
    , String revalDesc
    , String revalDate
    , String gvUserCd
){
    /**
     * 개선완료 커맨드 생성.
     * <p>REVAL_RISK_LV 은 클라가 보낸 revalRiskLv 를 신뢰하지 않고,
     * 서버에서 빈도×강도로 재계산한 값(recalcRiskLv)을 단일 출처로 저장한다(Low-1B).
     */
    public static ImprovementCompleteCommand from(ImprovementCompleteParam param, String recalcRiskLv) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new ImprovementCompleteCommand(
            param.gvCmpnyCd()
            , param.siteCd()
            , param.processCd()
            , param.assessmentCd()
            , param.revalLikelihoodScore()
            , param.revalSeverityScore()
            , recalcRiskLv
            , param.revalDesc()
            , param.revalDate()
            , param.gvUserCd()
        );
    }
}
