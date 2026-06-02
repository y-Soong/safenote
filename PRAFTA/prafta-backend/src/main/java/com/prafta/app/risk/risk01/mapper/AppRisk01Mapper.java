package com.prafta.app.risk.risk01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.risk.risk01.application.command.RiskAssessmentSaveCommand;
import com.prafta.app.risk.risk01.application.query.RiskTypeInfoQuery;
import com.prafta.app.risk.risk01.result.RiskCategoryResult;
import com.prafta.app.risk.risk01.result.RiskHazardResult;
import com.prafta.app.risk.risk01.result.RiskTypeResult;
import com.prafta.common.dto.TokenInfo;

/**
 * prafta-036-B2: 앱 위험성평가(risk01) Mapper.
 * <p>chkLst01 메서드(selectChkLstInfo / mergeChkptInspectAnswer) 잘못된 복붙 제거 완료.
 *   정식 위치: com.prafta.app.chkLst.chkLst01.mapper.AppChkLst01Mapper
 */
@Mapper
public interface AppRisk01Mapper {

    /**
     * 위험성평가 구분(공정/COM002) 조회.
     */
    List<RiskCategoryResult> selectRiskCategory(
            @Param(value = "param") RiskTypeInfoQuery query
            , @Param(value = "token") TokenInfo tokenInfo
    );

    /**
     * 위험성평가 분류(공정별 위험요인) 조회.
     */
    List<RiskTypeResult> selectRiskType(
            @Param(value = "param") RiskTypeInfoQuery query
            , @Param(value = "token") TokenInfo tokenInfo
    );

    /**
     * 위험성평가 발생상황(유해요인) 조회.
     */
    List<RiskHazardResult> selectRiskHazard(
            @Param(value = "param") RiskTypeInfoQuery query
            , @Param(value = "token") TokenInfo tokenInfo
    );

    /**
     * 위험성평가 UPSERT (INSERT ... ON DUPLICATE KEY UPDATE).
     */
    void mergeRiskAssessment(
            @Param(value = "param") RiskAssessmentSaveCommand command
            , @Param(value = "token") TokenInfo tokenInfo
    );
}
