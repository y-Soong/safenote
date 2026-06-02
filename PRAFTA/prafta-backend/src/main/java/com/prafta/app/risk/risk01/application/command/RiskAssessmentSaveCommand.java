package com.prafta.app.risk.risk01.application.command;

import com.prafta.app.risk.risk01.application.param.RiskAssessmentSaveParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-036-B2: 위험성평가 저장 Command (mapper.mergeRiskAssessment 진입).
 *
 * <p>SQL 본문은 변경하지 않음 — 기존 parameter key 와 1:1 일치하도록 컴포넌트 명을 동일하게 유지한다.
 * <pre>
 *   #{param.siteCd}, #{param.processCd}, #{param.riskTypeCd}, #{param.hazardCd}
 *   #{param.assessmentDesc}, #{param.assessmentStatus}
 *   #{param.initLikelihoodScore}, #{param.initSeverityScore}
 *   #{param.initDesc}, #{param.initFileMgmtCd}
 * </pre>
 * INIT_RISK_LV 컬럼은 SQL 의 IFNULL(initLikelihoodScore,1) * IFNULL(initSeverityScore,1) 로 계산되므로
 *   initRiskLv 필드는 본 command 에 포함하지 않는다 (기존 SQL 도 사용하지 않음).
 */
public record RiskAssessmentSaveCommand(
    String siteCd
    , String processCd
    , String riskTypeCd
    , String hazardCd
    , String assessmentDesc
    , String assessmentStatus
    , String initLikelihoodScore
    , String initSeverityScore
    , String initDesc
    , String initFileMgmtCd
) {
    /**
     * 정적 팩토리: 저장 Param + 파일관리코드(없으면 빈 문자열) -> Command.
     * <p>assessmentStatus 는 기존 동작 보존 — '001'(검토요청) 고정.
     */
    public static RiskAssessmentSaveCommand from(RiskAssessmentSaveParam param, String fileMgmtCd) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new RiskAssessmentSaveCommand(
            param.siteCd()
            , param.processCd()
            , param.riskTypeCd()
            , param.hazardCd()
            , param.assessmentDesc()
            , "001"     // 검토요청 상태 (기존 동작 보존)
            , param.initLikelihoodScore()
            , param.initSeverityScore()
            , param.initDesc()
            , fileMgmtCd
        );
    }
}
