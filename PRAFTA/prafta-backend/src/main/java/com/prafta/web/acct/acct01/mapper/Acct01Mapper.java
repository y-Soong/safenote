package com.prafta.web.acct.acct01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.acct.acct01.application.command.AcctInsertCommand;
import com.prafta.web.acct.acct01.application.command.LinkQueryContext;
import com.prafta.web.acct.acct01.application.param.AcctInfoParam;
import com.prafta.web.acct.acct01.application.param.AcctListParam;
import com.prafta.web.acct.acct01.application.param.AcctUpdateParam;
import com.prafta.web.acct.acct01.application.param.ChkptOptionParam;
import com.prafta.web.acct.acct01.application.param.LegalStepListParam;
import com.prafta.web.acct.acct01.application.param.LegalStepSaveParam;
import com.prafta.web.acct.acct01.application.param.LinkSnapshotParam;
import com.prafta.web.acct.acct01.application.param.RiskCategoryOptionParam;
import com.prafta.web.acct.acct01.application.param.VictimSearchParam;
import com.prafta.web.acct.acct01.result.AcctResult;
import com.prafta.web.acct.acct01.result.AttendanceLinkResult;
import com.prafta.web.acct.acct01.result.ChkptOptionResult;
import com.prafta.web.acct.acct01.result.LegalStepHistoryResult;
import com.prafta.web.acct.acct01.result.LegalStepProgressResult;
import com.prafta.web.acct.acct01.result.LegalStepResult;
import com.prafta.web.acct.acct01.result.LinkSnapshotResult;
import com.prafta.web.acct.acct01.result.PatrolItemResult;
import com.prafta.web.acct.acct01.result.PatrolLinkResult;
import com.prafta.web.acct.acct01.result.RiskAssessmentDetailResult;
import com.prafta.web.acct.acct01.result.RiskCategoryOptionResult;
import com.prafta.web.acct.acct01.result.RiskLinkResult;
import com.prafta.web.acct.acct01.result.ScheduleLinkResult;
import com.prafta.web.acct.acct01.result.TbmLinkResult;
import com.prafta.web.acct.acct01.result.VictimResult;

@Mapper
public interface Acct01Mapper {

    // 사업장 접근 권한 확인 (tb_user_site_auth, USE_YN='Y'). 1 이상이면 접근 가능
    int countUserSiteAuth(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("userCd") String userCd
        , @Param("siteCd") String siteCd
    );

    // ── 048-03 CRUD ──────────────────────────────────────────────
    // 사고 목록 (사업장 스코프 + 등급/상태/기간 필터)
    List<AcctResult> selectAcctList(AcctListParam param);

    // 사고 단건 상세 (사업장 스코프 강제)
    AcctResult selectAcctInfo(AcctInfoParam param);

    // 사고 헤더 행 잠금(FOR UPDATE). 법정단계 저장 + 처리상태 파생을 사고 단위로 직렬화. 없으면 null
    String lockAcctHeader(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
    );

    // 연계 조회용 사고 헤더(발생일/시각/재해자/유형/사업장) 단건. 사업장 스코프 강제
    AcctResult selectAcctHeader(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
    );

    // 채번: ACC + YYYYMMDD(발생일) + 4자리 SEQ (사업장+발생일 기준 MAX+1)
    String selectNextAcctId(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("occurYmd") String occurYmd
    );

    // 사고 INSERT
    int insertAcct(AcctInsertCommand command);

    // 사고 UPDATE (등급/경위/상태/장소 등)
    int updateAcct(AcctUpdateParam param);

    // 사고 soft delete (DEL_YN='Y')
    int deleteAcct(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("gvUserCd") String gvUserCd
    );

    // 재해자 검색 (정규 tb_user + 일용 tb_daily_user UNION, 사업장 스코프)
    List<VictimResult> selectVictimList(VictimSearchParam param);

    // 재해자 실재/소속 검증 (등록 시): victimUserTypeCd 분기로 tb_user(REGULAR) / tb_daily_user(DAILY)에서
    // cmpnyCd + siteCd + userCd 매칭 COUNT. 0 이면 유령/타 사업장 참조로 차단.
    int countVictim(
        @Param("cmpnyCd") String cmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("victimUserTypeCd") String victimUserTypeCd
        , @Param("victimUserCd") String victimUserCd
    );

    // ── 048-04 연계 조회 ─────────────────────────────────────────
    // 근태: 정규직 당일 스케줄 (tb_user_work_plan + tb_sch_mgmt). 일용직은 빈 결과
    ScheduleLinkResult selectAttendanceSchedule(LinkQueryContext ctx);

    // 근태: 당일 실근태 (tb_user_attd_mgmt, WORK_SEQ 단위)
    List<AttendanceLinkResult> selectAttendanceRecords(LinkQueryContext ctx);

    // 순회점검: 점검대상별 1주일 양호/불량 집계
    List<PatrolLinkResult> selectPatrolSummary(LinkQueryContext ctx);

    // 순회점검: 불량 항목 상세 (집계 드릴다운)
    List<PatrolItemResult> selectPatrolBadItems(LinkQueryContext ctx);

    // 위험성평가: 3계층 매칭 + 사고일-3M ~ 사고일 유효 평가
    List<RiskLinkResult> selectRiskList(LinkQueryContext ctx);

    // T8: 위험성평가 출력 보강 — 요청 assessmentCd 가 해당 사고의 RISK 연계에 실제 등록됐는지 정확 매칭 COUNT(IDOR 가드)
    int selectAcctLinkAssessmentCnt(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("assessmentCd") String assessmentCd
    );

    // T8: 위험성평가 출력 상세(개선실행계획서/개선완료보고서용). 사업장 스코프 강제
    RiskAssessmentDetailResult selectRiskAssessmentDetailForPrint(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("assessmentCd") String assessmentCd
    );

    // TBM: 당일 세션 + 재해자 이수여부
    List<TbmLinkResult> selectTbmList(LinkQueryContext ctx);

    // 점검대상 검색 옵션 (ChkptSearchPop)
    List<ChkptOptionResult> selectChkptOptions(ChkptOptionParam param);

    // 위험성평가 3계층 옵션 (공정/위험요인구분/유해요인)
    List<RiskCategoryOptionResult> selectRiskCategoryOptions(RiskCategoryOptionParam param);

    // ── 048-05 스냅샷/법정절차 ───────────────────────────────────
    // 확정 스냅샷 조회 (tb_acct_link)
    List<LinkSnapshotResult> selectLinkSnapshots(LinkSnapshotParam param);

    // 재확정 전 해당 사고+도메인 스냅샷 전체 삭제 (REPLACE 전략)
    int deleteLinkByDomain(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("linkDomainCd") String linkDomainCd
    );

    // 스냅샷 1행 INSERT
    int insertLink(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("linkDomainCd") String linkDomainCd
        , @Param("linkSeq") int linkSeq
        , @Param("linkKeyJson") String linkKeyJson
        , @Param("snapshotJson") String snapshotJson
        , @Param("gvUserCd") String gvUserCd
    );

    // ②탭 법정절차 + 진행상태 (master LEFT JOIN tb_acct_legal_step). 등급 기준 필터
    List<LegalStepResult> selectLegalStepList(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("acctGradeCd") String acctGradeCd
    );

    // ②탭 조치완료 체크/비고 UPSERT
    int upsertLegalStep(LegalStepSaveParam param);

    // 처리상태 파생용 진행 집계 (PROCESS 단계 총수/완료수, 등급 OR ALL). selectLegalStepList 와 동일 술어 유지
    LegalStepProgressResult selectLegalStepProgress(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("acctGradeCd") String acctGradeCd
    );

    // 처리상태 파생 갱신 (saveLegalStep 전용. processStatusCd 는 서버 파생값만 바인딩)
    int updateAcctProcessStatus(
        @Param("gvCmpnyCd") String gvCmpnyCd
        , @Param("siteCd") String siteCd
        , @Param("acctId") String acctId
        , @Param("processStatusCd") String processStatusCd
        , @Param("gvUserCd") String gvUserCd
    );

    // ③탭 처리 이력 파생 롤업 (IS_DONE_YN='Y' 완료절차 시간순)
    List<LegalStepHistoryResult> selectLegalStepHistory(LegalStepListParam param);
}
