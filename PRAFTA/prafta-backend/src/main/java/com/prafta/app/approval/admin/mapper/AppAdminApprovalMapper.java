package com.prafta.app.approval.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.approval.admin.application.query.ApprovalScopeQuery;
import com.prafta.app.approval.admin.result.AttdSnapshotRow;
import com.prafta.app.approval.admin.result.HistoryRow;
import com.prafta.app.approval.admin.result.LeaveBalanceRow;
import com.prafta.app.approval.admin.result.LeaveBodyRow;
import com.prafta.app.approval.admin.result.NeighborAttdSegmentRow;
import com.prafta.app.approval.admin.result.PendingCorrOtRow;
import com.prafta.app.approval.admin.result.PendingLeaveRow;
import com.prafta.app.approval.admin.result.ReqMetaRow;
import com.prafta.app.approval.admin.result.SchedBodyRow;

/**
 * 001-P2: 앱 관리자 승인 관리 Mapper(web reqinbox/leaveflow/attd07 조회 SQL 포팅 + 토큰·노드 스코프).
 *
 * <p>식별자(cmpny/user/site)는 서비스에서 토큰값만 전달한다. 노드 스코프는 Phase 1
 * {@code AdminScopeMapper.selectScopedNodeCds}(재귀 CTE) 결과 List 를 IN 절에 임베드한다.
 * <p>web comApi/webApi 컨트롤러·서비스를 직접 호출하지 않고 SQL 만 본 매퍼로 포팅했다(앱/웹 분리).
 */
@Mapper
public interface AppAdminApprovalMapper {

    // ============================ A-1 대기 리스트 ============================

    /** 근태보정/초과 대기('01') 요청 목록(스코프 + 키워드 + 페이징). */
    List<PendingCorrOtRow> selectPendingCorrOt(ApprovalScopeQuery query);

    /** 근태보정/초과 대기 건수(스코프 + 키워드). */
    int countPendingCorrOt(ApprovalScopeQuery query);

    /** 연차(05/06) 대기 목록 — 내가 현재 단계 결재자인 요청(결재선 기반). */
    List<PendingLeaveRow> selectPendingLeave(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("approverUserCd") String approverUserCd,
                                             @Param("keyword") String keyword,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /** 연차 대기 건수(내가 현재 단계 결재자). */
    int countPendingLeave(@Param("cmpnyCd") String cmpnyCd,
                          @Param("approverUserCd") String approverUserCd,
                          @Param("keyword") String keyword);

    // ============================ A-2 상세 ============================

    /** 요청 메타 1건(회사 스코프). 없으면 null. 요청자 NODE_CD(fallback)·이름 포함. */
    ReqMetaRow selectReqMeta(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);

    /** 근태보정 Before 스냅샷(TARGET_ID 의 현재 출퇴근). 없으면 null. */
    AttdSnapshotRow selectAttdSnapshot(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("attdId") String attdId);

    /**
     * 겹침가드 개선(2026-08-06): 대상 근무일 앞뒤(D-1 / D+1)의 활성 근태 구간 — 근태보정 승인 상세 표시용.
     *
     * <p>웹 {@code Attd07Mapper.selectAttdSegmentsAroundDayExcept} 의 의도적 미러다
     *   (app 패키지에서 web 매퍼를 주입한 선례가 없어 계층 규약을 유지한다).
     *   당일(baseYmd) 행은 SQL 에서 제외하고, WHERE 에 회사/사업장/사용자 스코프를 반드시 건다(cross-site IDOR 이중 차단).
     */
    List<NeighborAttdSegmentRow> selectNeighborAttdSegments(@Param("cmpnyCd") String cmpnyCd,
                                                            @Param("siteCd") String siteCd,
                                                            @Param("userCd") String userCd,
                                                            @Param("fromYmd") String fromYmd,
                                                            @Param("toYmd") String toYmd,
                                                            @Param("baseYmd") String baseYmd);

    /** 연차 본문(05=요청 연결 사용기록 / 06=수정대상 사용기록). 없으면 null. */
    LeaveBodyRow selectLeaveBody(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("reqId") String reqId,
                                 @Param("targetId") String targetId);

    /** 연차 잔여(활성 부여 합계). */
    LeaveBalanceRow selectLeaveBalance(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd,
                                       @Param("leaveCd") String leaveCd);

    /**
     * PRAFTA-APP-029-2(D6): 스케줄 수정(10) 상세 본문 — 현재 스케줄(WORK_PLAN_CD) + 요청 스케줄(REQ.SCH_CD)
     * 을 TB_SCH_MGMT 조인해 시각 range 로 산출(1행). 현재 근무계획 없거나 LEAVE_CD 면 cur* NULL.
     */
    SchedBodyRow selectSchedBody(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("siteCd") String siteCd,
                                 @Param("userCd") String userCd,
                                 @Param("workYmd") String workYmd,
                                 @Param("reqSchCd") String reqSchCd);

    /**
     * 마감 차단 판정(web AttdCloseMapper.countCovering 포팅): 대상 NODE_CD 의 자기/상위(INC_SUB)/전체('*')
     * 마감 행이 CLOSED 면 >0. nodeCd='*' 이면 전체사업장 마감만 검사.
     */
    int countCloseCovering(@Param("cmpnyCd") String cmpnyCd,
                           @Param("siteCd") String siteCd,
                           @Param("nodeCd") String nodeCd,
                           @Param("closeYm") String closeYm);

    // ============================ A-5 이력 ============================

    /** 근태보정/초과 처리완료(02/03/04) 이력 목록(스코프 + 기간 + 키워드 + 페이징, PROCESS_DATE DESC). */
    List<HistoryRow> selectHistoryCorrOt(ApprovalScopeQuery query);

    /** 근태보정/초과 처리완료 이력 건수. */
    int countHistoryCorrOt(ApprovalScopeQuery query);

    /** 연차 처리완료 이력 목록 — 내가 처리(결재선 참여)한 연차 요청(PROCESS_DATE DESC). */
    List<HistoryRow> selectHistoryLeave(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("approverUserCd") String approverUserCd,
                                        @Param("keyword") String keyword,
                                        @Param("startYmd") String startYmd,
                                        @Param("endYmd") String endYmd,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /** 연차 처리완료 이력 건수. */
    int countHistoryLeave(@Param("cmpnyCd") String cmpnyCd,
                          @Param("approverUserCd") String approverUserCd,
                          @Param("keyword") String keyword,
                          @Param("startYmd") String startYmd,
                          @Param("endYmd") String endYmd);

    // ============================ A-3 처리 보조 ============================

    /**
     * Fix3(F1): 노드관리자가 NODE_CD NULL 인 근태보정/초과 요청을 처리하기 직전, REQ.NODE_CD 를 요청자 실제 노드로 백필한다.
     *   app isInScope 가 COALESCE(REQ.NODE_CD, 요청자노드) 로 이미 권위 검증했으므로 데이터품질 교정에 해당한다.
     *   NODE_CD 가 NULL 인 행만 갱신(기존 값 미손상)하며, 처리 트랜잭션과 동일 경계에서 UPDATE_NO/UPDATE_DATE 를 남긴다.
     *
     * @return 갱신된 행 수(0=이미 NODE_CD 보유 등).
     */
    int backfillReqNodeCd(@Param("cmpnyCd") String cmpnyCd,
                          @Param("reqId") String reqId,
                          @Param("nodeCd") String nodeCd,
                          @Param("updateNo") String updateNo);
}
