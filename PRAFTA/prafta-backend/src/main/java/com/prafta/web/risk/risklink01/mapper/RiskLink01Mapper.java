package com.prafta.web.risk.risklink01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.risk.risklink01.application.command.NearMissLinkCommand;
import com.prafta.web.risk.risklink01.application.query.AvailableNearMissQuery;
import com.prafta.web.risk.risklink01.application.query.LinkedNearMissQuery;
import com.prafta.web.risk.risklink01.result.LinkNearMissResult;

@Mapper
public interface RiskLink01Mapper {

    // 사업장 접근 권한 확인 (tb_user_site_auth 매핑, USE_YN='Y'). 1 이상이면 접근 가능
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // L1 연결 후보(완료400 + 같은 사업장 + 미연결 + 검색어) 목록
    List<LinkNearMissResult> selectAvailableNearMiss(AvailableNearMissQuery query);

    // L2 연결됨(USE_YN='Y') 아차사고 목록
    List<LinkNearMissResult> selectLinkedNearMiss(LinkedNearMissQuery query);

    // L3/L4 대상 위험성평가 건의 진행상태 조회 (없으면 null). 003 전 편집 가능 판정용
    String selectAssessmentStatus(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("processCd") String processCd
        , @Param("assessmentCd") String assessmentCd
    );

    // L3 대상 아차사고의 처리상태 조회 (없으면 null). 완료(400) 검증용
    String selectNearMissReportStatus(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("nearMissId") String nearMissId
    );

    // L3 연결 행 존재 여부 (USE_YN 무관). upsert 분기용. 1 이상이면 행 존재
    int countLink(NearMissLinkCommand command);

    // L3 연결 INSERT (USE_YN='Y')
    int insertLink(NearMissLinkCommand command);

    // L3 연결 복원 (기존 행 USE_YN='Y')
    int restoreLink(NearMissLinkCommand command);

    // L4 연결 해제 (soft delete, USE_YN='N')
    int updateLinkUseYn(NearMissLinkCommand command);
}
