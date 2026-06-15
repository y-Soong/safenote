package com.prafta.app.safety.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.safety.admin.application.command.RiskStatusCommand;
import com.prafta.app.safety.admin.application.query.InspectionDetailQuery;
import com.prafta.app.safety.admin.application.query.InspectionListQuery;
import com.prafta.app.safety.admin.application.query.RiskDetailQuery;
import com.prafta.app.safety.admin.application.query.RiskFindingListQuery;
import com.prafta.app.safety.admin.result.InspectionAnswerResult;
import com.prafta.app.safety.admin.result.InspectionPointResult;
import com.prafta.app.safety.admin.result.RiskAssessmentResult;
import com.prafta.app.safety.admin.result.RiskStatusMetaResult;

/**
 * 관리자 안전 관리(순회점검/위험성평가) 매퍼 (prafta-app-025 J1-6).
 *
 * <p>웹 chkLst03/risk03 SQL 을 앱에 포팅하되 사업장/권한 스코프를 강제하고, 사진 경로는
 *    FNC_CMM_INFO_SRCH('FILE_PATH') 로 해석한다(평문 FILE_PATH 직노출/IDOR 비상속).
 */
@Mapper
public interface AppAdminSafetyMapper {

    /** 사업장 접근 권한 확인(tb_user_site_auth, USE_YN='Y'). 1 이상이면 접근 가능. */
    int countUserSiteAuth(@Param("cmpnyCd") String cmpnyCd,
                          @Param("userCd") String userCd,
                          @Param("siteCd") String siteCd);

    /** H1 순회점검 결과 리스트(포인트별 점검일수/불량수 — 단일 사업장 스코프 + 월 필터). */
    List<InspectionPointResult> selectInspectionList(InspectionListQuery query);

    /** H2 순회점검 상세(일자별 답변 + 불량 사진/비고 — CMPNY+SITE+CHKPT 스코프). */
    List<InspectionAnswerResult> selectInspectionDetailAnswers(InspectionDetailQuery query);

    /** H3 위험성평가 목록(사업장 스코프 강제 + 표시 필터). */
    List<RiskAssessmentResult> selectRiskFindingList(RiskFindingListQuery query);

    /** 상태전환 가드용 현재 상태 단건(PK 4키 + 사업장 스코프, 없으면 null). */
    RiskStatusMetaResult selectRiskFindingMeta(RiskDetailQuery query);

    /** H4 위험성평가 상세 단건(PK 4키 + 사업장 스코프, 없으면 null). */
    RiskAssessmentResult selectRiskFindingDetail(RiskDetailQuery query);

    /** H5 위험성평가 상태전환 UPDATE(WHERE 에 현재 상태 가드 — 영향 0건이면 동시전환 409). */
    int updateRiskStatus(RiskStatusCommand command);
}
