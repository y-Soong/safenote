package com.prafta.web.risk.riskimpr01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.risk.riskimpr01.application.command.ImprovementCompleteCommand;
import com.prafta.web.risk.riskimpr01.application.command.ImprovementItemCommand;
import com.prafta.web.risk.riskimpr01.application.query.ImprovementItemListQuery;
import com.prafta.web.risk.riskimpr01.result.ImprovementItemResult;

@Mapper
public interface RiskImpr01Mapper {

    // 사업장 접근 권한 확인 (tb_user_site_auth 매핑, USE_YN='Y'). 1 이상이면 접근 가능
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // 대상 위험성평가 건의 진행상태 조회 (없으면 null). 편집/완료 게이트 판정용
    String selectAssessmentStatus(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 개선항목 목록 (평가키 스코프, USE_YN='Y', 최신 SEQ 상단)
    List<ImprovementItemResult> selectImprovementItems(ImprovementItemListQuery query);

    // 신규 채번 (IFNULL(MAX(IMPROVEMENT_SEQ),0)+1, 평가키 스코프)
    int selectNextImprovementSeq(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 활성 개선항목 건수 (USE_YN='Y'). 개선완료 가드용
    int countActiveImprovementItems(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // 개선항목 INSERT (USE_YN='Y')
    int insertImprovementItem(ImprovementItemCommand command);

    // 개선항목 UPDATE (평가키 + SEQ + USE_YN='Y'). 0행 → 404
    int updateImprovementItem(ImprovementItemCommand command);

    // 개선항목 soft delete (USE_YN='N', 평가키 + SEQ + USE_YN='Y'). 0행 → 404
    int softDeleteImprovementItem(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
        , @Param("improvementSeq") Integer improvementSeq
        , @Param("userCd") String userCd
    );

    // 개선완료: tb_risk_assessment 진행상태 003 전이 + REVAL_* 동기화 (WHERE 005). 0행 → 422
    int updateAssessmentComplete(ImprovementCompleteCommand command);
}
