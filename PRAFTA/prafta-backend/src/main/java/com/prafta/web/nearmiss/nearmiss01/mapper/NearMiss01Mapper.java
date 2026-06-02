package com.prafta.web.nearmiss.nearmiss01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.nearmiss.nearmiss01.application.command.AssessmentTransferCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.ChangeStatusCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.InsertIncidentCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.SaveIncidentCommand;
import com.prafta.web.nearmiss.nearmiss01.application.query.IncidentInfoQuery;
import com.prafta.web.nearmiss.nearmiss01.application.query.IncidentListQuery;
import com.prafta.web.nearmiss.nearmiss01.application.query.NearMissIdSeqQuery;
import com.prafta.web.nearmiss.nearmiss01.result.IncidentResult;
import com.prafta.web.nearmiss.nearmiss01.result.StatusCountResult;

@Mapper
public interface NearMiss01Mapper {

    // 사업장 접근 권한 확인 (tb_user_site_auth 매핑, USE_YN='Y'). 1 이상이면 접근 가능
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // E1 목록 (사업장 스코프 + 상태/유형/잠재중대성/기간 필터)
    List<IncidentResult> selectIncidentList(IncidentListQuery query);

    // E2 단건 상세 (사업장 스코프 강제)
    IncidentResult selectIncidentInfo(IncidentInfoQuery query);

    // E3 상태별 카운트 (필터 동반)
    StatusCountResult selectStatusCounts(IncidentListQuery query);

    // E5 전이 검증용: 현재 처리상태 단건 조회 (없으면 null)
    String selectReportStatus(IncidentInfoQuery query);

    // E4 정밀조사 저장
    int updateIncident(SaveIncidentCommand command);

    // E5 상태 전환
    int updateStatus(ChangeStatusCommand command);

    // E6 채번: 사업장+당일 기준 시퀀스 다음값 (NM + YYYYMMDD + 3자리)
    String selectNextNearMissId(NearMissIdSeqQuery query);

    // E6 사건 INSERT
    int insertIncident(InsertIncidentCommand command);

    // E6 원 위험성평가 전환 가능 건 확인 (사업장 스코프 + 전환 허용 상태만). 1 이상이면 전환 가능
    int countAssessment(AssessmentTransferCommand command);

    // E6 원 위험성평가 단순 존재 확인 (상태 무관). 404 vs 422 구분용
    int countAssessmentAny(AssessmentTransferCommand command);

    // E6 원 위험성평가 상태 -> '005' 이관
    int transferAssessmentStatus(AssessmentTransferCommand command);
}
