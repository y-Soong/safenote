package com.prafta.web.subcon.subcon03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.subcon.subcon03.application.command.BundleInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.ShareReqInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.ShareReqProcessCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotAttdInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotNearmissInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotRiskImproveInsertCommand;
import com.prafta.web.subcon.subcon03.application.command.SnapshotRiskInsertCommand;
import com.prafta.web.subcon.subcon03.result.ChainSiteResult;
import com.prafta.web.subcon.subcon03.result.NearmissSourceRow;
import com.prafta.web.subcon.subcon03.result.RelayCandidateResult;
import com.prafta.web.subcon.subcon03.result.RiskImproveSourceRow;
import com.prafta.web.subcon.subcon03.result.RiskSourceRow;
import com.prafta.web.subcon.subcon03.result.ShareCmpnyResult;
import com.prafta.web.subcon.subcon03.result.ShareReqRaw;
import com.prafta.web.subcon.subcon03.result.ShareReqResult;
import com.prafta.web.subcon.subcon03.result.SiteNodeResult;
import com.prafta.web.subcon.subcon03.result.SnapshotDetailResult;
import com.prafta.web.subcon.subcon03.result.SnapshotNearmissDetailResult;
import com.prafta.web.subcon.subcon03.result.SnapshotResult;
import com.prafta.web.subcon.subcon03.result.SnapshotRiskDetailResult;
import com.prafta.web.subcon.subcon03.result.SnapshotRiskImproveResult;
import com.prafta.web.subcon.subcon03.result.SnapshotSourceRow;

@Mapper
public interface Subcon03Mapper {

    // =========================== 권한 게이트 ===========================

    /**
     * 메뉴 버튼 권한 보유 카운트(서버측 역할 게이트 — Subcon_03/Subcon_04 메뉴, Subcon02 패턴 미러).
     * btnType 은 서비스 상수('SRCH'/'NEW'/'SAVE')만 전달하며, XML 에서 고정 컬럼으로 분기(동적 ${} 미사용).
     */
    int selectMenuButtonAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("authCd") String authCd,
            @Param("menuDId") String menuDId, @Param("btnType") String btnType);

    // =========================== 목록/후보(T3-02) ===========================

    /** 자사가 당사자(요청측 또는 제공측)인 공유요청 전 상태 목록(목록=이력). 상한(limit) 필수. */
    List<ShareReqResult> selectShareReqList(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("limit") int limit);

    /** 관계 ACCEPTED 상대 회사 목록(요청 후보 ① — 회사코드/회사명만). */
    List<ShareCmpnyResult> selectActiveRelationCmpnyList(@Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * 선택한 제공사와 사업장 연동 체인(인접 1홉 양방향 — D1)이 있는 <b>내</b> 사업장 목록(요청 후보 ②).
     * 제공측 사업장 코드/명은 응답하지 않는다.
     */
    List<ChainSiteResult> selectChainSiteList(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("prvCmpnyCd") String prvCmpnyCd);

    // =========================== 생성 가드(T3-02, §5-3) ===========================

    /** (cmpnyCdA, cmpnyCdB) 쌍의 ACCEPTED 관계ID(방향 불문). 미수립이면 null. */
    Long selectActiveRelationId(@Param("cmpnyCdA") String cmpnyCdA, @Param("cmpnyCdB") String cmpnyCdB);

    /** 요청측 소유 활성 사업장 존재 카운트(소유 검증 — 타사 사업장 주입 봉인). */
    int selectMySiteActiveCnt(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * 사업장 체인 해석(§5-2) — 내 사업장(siteCd)에 대응하는 <b>제공측 테넌트</b> 사업장코드.
     * ① 내가 원본 → 제공사가 미러(tb_site_link ACTIVE 의 DST_SITE_CD)
     * ② 제공사가 원본 → 내가 미러(tb_site.LINK_SRC_SITE_CD)
     * 둘 다 아니면 null(= 연동된 사업장 아님). 클라 입력 TARGET_SITE_CD 는 받지 않는다.
     */
    String selectTargetSiteCdByChain(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("siteCd") String siteCd,
            @Param("prvCmpnyCd") String prvCmpnyCd);

    /** 동일 조건 REQUESTED 중복 카운트(연타 차단 — 종결 상태 행은 재요청 허용이라 세지 않는다). */
    int selectRequestedDupCnt(@Param("reqCmpnyCd") String reqCmpnyCd, @Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("dataType") String dataType,
            @Param("periodStr") String periodStr, @Param("periodEnd") String periodEnd);

    /** 공유요청 INSERT(STATUS='REQUESTED' 고정, useGeneratedKeys 로 shareReqId 회수). */
    void insertShareReq(ShareReqInsertCommand command);

    // =========================== 상태 전이(T3-02/04, §4) ===========================

    /** 취소: REQUESTED→CANCELLED. 당사자 조건 REQ_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int cancelShareReq(ShareReqProcessCommand command);

    /** 거부: REQUESTED→REJECTED. 당사자 조건 PRV_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int rejectShareReq(ShareReqProcessCommand command);

    /** 승인 선점: REQUESTED→APPROVED. 당사자 조건 PRV_CMPNY_CD=actor. 영향행 수 반환(0=404 — 동시 승인 차단). */
    int approveShareReqPreempt(ShareReqProcessCommand command);

    /** 공유요청 원시행(내부 전용 — 선점 성공 직후 스냅샷 생성 근거). */
    ShareReqRaw selectShareReqById(@Param("shareReqId") Long shareReqId);

    /** 승인 사전정보용 원시행 — 제공측 소속 + REQUESTED 조건 포함(비당사자는 null → 404 통합). */
    ShareReqRaw selectRequestedShareReqForProvider(@Param("shareReqId") Long shareReqId,
            @Param("prvCmpnyCd") String prvCmpnyCd);

    /** 관계 ACCEPTED 존속 카운트(승인 시점 재검증 — 해지 레이스 차단). */
    int selectRelationAcceptedCnt(@Param("relationId") Long relationId);

    // =========================== 마감 게이팅(T3-03, §5-4) ===========================

    /**
     * 대상 사업장의 부서노드 코드 전수(마감 커버리지 검사 대상 — 전체 센티넬 '*' 는 서비스가 추가).
     * [T1 게이트 완화] 활성 소속 사용자(USE_YN='Y', 미탈퇴) 0명인 빈 부서는 제외된다.
     * 게이트(월 완전마감 판정) 전용 — 행 단위 판정용 {@link #selectSiteNodeList} 와 혼용 금지.
     */
    List<String> selectSiteNodeCdList(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * [PS-03] 대상 사업장 전체 노드 목록(코드+명, 무필터) — 행 단위 커버리지 판정의 노드 유효성
     * (고아 여부)과 제외 부서명 표기에 사용한다.
     */
    List<SiteNodeResult> selectSiteNodeList(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * 해당 월에 <b>부서노드 마감으로는 덮이지 않는</b> 근태/초과근무 행 수.
     *
     * <p>NODE_CD 가 NULL/공백이거나(일용직 등) 사업장 노드 트리에 없는 고아 값이면, 부서별 마감을
     * 아무리 촘촘히 해도 그 행은 커버되지 않는다 → 이런 행이 있는 달은 전체 센티넬('*') 마감을 요구한다.
     * 0건이면 전 부서노드 마감만으로 그 달을 마감 완료로 인정한다(부서별 마감 운영 사업장 지원).
     */
    int countNodeUncoveredAttdRows(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("closeYm") String closeYm);

    // =========================== 스냅샷 생성(T3-04, §5-5·§5-6) ===========================

    /** 회사명(소속표시 스냅샷용 — 제공사 자신). */
    String selectCmpnyNm(@Param("cmpnyCd") String cmpnyCd);

    /** 사업장명(자기 테넌트 사업장만 조회 — 화면 표시용). */
    String selectSiteNm(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * VERSION 채번 근거(D5) — 동일 조건 키(요청사·제공사·대상사업장·유형·기간)의 기존 최대 VERSION.
     * 재요청이 새 SHARE_REQ_ID 라 SHARE_REQ_ID 만으로는 VERSION 이 항상 1이 된다.
     *
     * <p>COUNT 가 아니라 MAX 를 쓴다 — 서로 다른 SHARE_REQ_ID 두 건이 동시 승인될 때 COUNT 기반이면
     * 같은 VERSION 이 중복 채번된다(UNIQUE 가 (SHARE_REQ_ID, VERSION) 이라 DB 도 막지 못함).
     */
    int selectMaxSnapshotVersionByCondition(@Param("reqCmpnyCd") String reqCmpnyCd, @Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("dataType") String dataType,
            @Param("periodStr") String periodStr, @Param("periodEnd") String periodEnd);

    /** 근태행 원천(ATTD) — 스케줄 effective-dating + attd08 판정식 + 당일 OT/연차 결합. */
    List<SnapshotSourceRow> selectAttdSourceRows(@Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("periodStr") String periodStr,
            @Param("periodEnd") String periodEnd);

    /** 근태행이 없는 초과근무 원천(OT_ONLY — 휴일근무 등. 없으면 원청이 결근으로 오독한다). */
    List<SnapshotSourceRow> selectOtOnlySourceRows(@Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("periodStr") String periodStr,
            @Param("periodEnd") String periodEnd);

    /** 근태행이 없는 연차 원천(LEAVE_ONLY — 종일/다일 연차). */
    List<SnapshotSourceRow> selectLeaveOnlySourceRows(@Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("periodStr") String periodStr,
            @Param("periodEnd") String periodEnd);

    /** 스냅샷 헤더 INSERT(OWNER_CMPNY_CD 는 DB 의 REQ_CMPNY_CD 에서만 주입). useGeneratedKeys 로 snapshotId 회수. */
    void insertSnapshot(SnapshotInsertCommand command);

    /** 상세행 배치 INSERT(서비스가 청크 분할 호출). 영향행 수 반환. */
    int insertSnapshotAttdRows(@Param("rows") List<SnapshotAttdInsertCommand> rows);

    /** 상세행 총 건수 확정(스냅샷 불변 원칙의 유일한 예외 — 생성 트랜잭션 내 1회). */
    int updateSnapshotRowCnt(@Param("snapshotId") Long snapshotId, @Param("rowCnt") int rowCnt,
            @Param("updateNo") String updateNo);

    /** 스냅샷 내 현재 최대 WORKER_SEQ(릴레이 복사 오프셋 — 인물 번호 충돌 방지). 없으면 0. */
    int selectMaxWorkerSeq(@Param("snapshotId") Long snapshotId);

    // =========================== 릴레이(T3-05, §5-7) ===========================

    /**
     * 릴레이 후보 — 승인자(제공측)가 수신 보유 중인 스냅샷 중 4조건 충족분.
     * ① OWNER_CMPNY_CD = 승인자, ② 하위 요청의 REQ_SITE_CD = 지금 요청의 TARGET_SITE_CD(같은 사업장 체인),
     * ③ 동일 DATA_TYPE + 기간 ⊆ 요청 기간, ④ 지금 요청이 CLOSED_ONLY='Y' 면 미마감 미포함분만.
     * 하위 제공사 회사코드/회사명은 응답하지 않는다.
     */
    List<RelayCandidateResult> selectRelayCandidates(@Param("ownerCmpnyCd") String ownerCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("dataType") String dataType,
            @Param("periodStr") String periodStr, @Param("periodEnd") String periodEnd,
            @Param("closedOnlyYn") String closedOnlyYn);

    /**
     * 릴레이 relabel 복사 — 하위 스냅샷 상세행을 새 스냅샷으로 INSERT ... SELECT.
     * AFFIL_CMPNY_NM 은 제공사(승인자) 회사명으로 덮어쓰고, WORKER_SEQ 는 오프셋을 더해 재채번한다.
     * 하위 스냅샷ID/하위 회사 식별자는 어떤 컬럼에도 넣지 않는다. 소유 검증(OWNER_CMPNY_CD)은 SQL 내부.
     */
    int copyRelayRows(@Param("snapshotId") Long snapshotId, @Param("srcSnapshotId") Long srcSnapshotId,
            @Param("ownerCmpnyCd") String ownerCmpnyCd, @Param("affilCmpnyNm") String affilCmpnyNm,
            @Param("workerSeqOffset") int workerSeqOffset, @Param("insertNo") String insertNo);

    /** 릴레이 묶음 감사 INSERT(제공측 테넌트 전용 — 수신측 조회 API 는 조인하지 않는다). */
    void insertSnapshotBundle(BundleInsertCommand command);

    // =========================== 수신 자료 조회(T3-06, §5-8) ===========================

    /** 자사 소유(OWNER_CMPNY_CD=gv) 스냅샷 목록 + 연동 종료 표식(relationActiveYn). */
    List<SnapshotResult> selectSnapshotList(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("limit") int limit);

    /** 수신 스냅샷 상세행(읽기전용 페이징). 소유 검증(EXISTS OWNER_CMPNY_CD=gv)은 SQL 안에서 강제. */
    List<SnapshotDetailResult> selectSnapshotDetail(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("snapshotId") Long snapshotId, @Param("offset") long offset, @Param("limit") int limit);

    // =========================== T7 위험성평가(RISK) 스냅샷 생성 ===========================

    /** 확정 위험성평가 원천(제공사·대상 사업장·기간 INIT_ASSESS_DATE·상태 IN statusList). 명칭 스냅샷 + 성명 평문. */
    List<RiskSourceRow> selectRiskSourceRows(@Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("periodStr") String periodStr,
            @Param("periodEnd") String periodEnd, @Param("statusList") List<String> statusList);

    /** 위 확정 평가에 매달린 활성 개선항목(USE_YN='Y') — (PROCESS_CD, ASSESSMENT_CD)로 부모 그룹핑. */
    List<RiskImproveSourceRow> selectRiskImproveSourceRows(@Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("periodStr") String periodStr,
            @Param("periodEnd") String periodEnd, @Param("statusList") List<String> statusList);

    /** 위험성평가 상세행 INSERT(단건 — 개선항목 부모 DETAIL_ID 회수용, useGeneratedKeys). */
    void insertSnapshotRisk(SnapshotRiskInsertCommand command);

    /** 위험성평가 개선항목 자식행 배치 INSERT. 영향행 수 반환. */
    int insertSnapshotRiskImproveRows(@Param("rows") List<SnapshotRiskImproveInsertCommand> rows);

    /** 스냅샷 내 최대 RISK ROW_SEQ(릴레이 표시순번 오프셋). 없으면 0. */
    int selectMaxRiskRowSeq(@Param("snapshotId") Long snapshotId);

    /** 스냅샷 내 최대 RISK ASSESSOR_SEQ(릴레이 작성자 번호 충돌 방지 오프셋). 없으면 0. */
    int selectMaxRiskAssessorSeq(@Param("snapshotId") Long snapshotId);

    // =========================== T7 아차사고(NEARMISS) 스냅샷 생성 ===========================

    /** 확정 아차사고 원천(제공사·대상 사업장·기간 OCCUR_DTIME·상태 IN statusList·USE_YN='Y'). 명칭 스냅샷 + 성명 평문. */
    List<NearmissSourceRow> selectNearmissSourceRows(@Param("prvCmpnyCd") String prvCmpnyCd,
            @Param("targetSiteCd") String targetSiteCd, @Param("periodStr") String periodStr,
            @Param("periodEnd") String periodEnd, @Param("statusList") List<String> statusList);

    /** 아차사고 상세행 배치 INSERT. 영향행 수 반환. */
    int insertSnapshotNearmissRows(@Param("rows") List<SnapshotNearmissInsertCommand> rows);

    /** 스냅샷 내 최대 NEARMISS ROW_SEQ(릴레이 표시순번 오프셋). 없으면 0. */
    int selectMaxNearmissRowSeq(@Param("snapshotId") Long snapshotId);

    /** 스냅샷 내 최대 NEARMISS REPORTER_SEQ(릴레이 제보자 번호 충돌 방지 오프셋). 없으면 0. */
    int selectMaxNearmissReporterSeq(@Param("snapshotId") Long snapshotId);

    // =========================== T7 수신 상세 조회 + 첨부 서빙 ===========================

    /** 위험성평가 수신 상세(부모행) — 소유 검증(EXISTS OWNER=gv)은 SQL 내부. 릴레이 복사도 이 쿼리 재사용. */
    List<SnapshotRiskDetailResult> selectSnapshotRiskRows(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("snapshotId") Long snapshotId);

    /** 위험성평가 수신 상세(개선항목 자식) — 소유 검증(EXISTS OWNER=gv)은 SQL 내부. */
    List<SnapshotRiskImproveResult> selectSnapshotRiskImproveRows(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("snapshotId") Long snapshotId);

    /** 아차사고 수신 상세 — 소유 검증(EXISTS OWNER=gv)은 SQL 내부. 릴레이 복사도 이 쿼리 재사용. */
    List<SnapshotNearmissDetailResult> selectSnapshotNearmissRows(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("snapshotId") Long snapshotId);

    /**
     * 첨부 서빙 소유+참조 검증(§5-9) — snapshotId OWNER=gv 이고 fileMgmtCd 가 그 스냅샷 상세행/개선항목의 첨부
     * 집합(RISK init/reval + RISK_IMPROVE + NEARMISS)에 존재하면 참조 수(양수), 아니면 0. 소유 실패 시 null.
     */
    Integer selectSnapshotFileRefCnt(@Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("snapshotId") Long snapshotId, @Param("fileMgmtCd") String fileMgmtCd);

    // =========================== 관계 해지 훅(T3-07) ===========================

    /** 관계 산하 진행중(REQUESTED) 공유요청 건수(해지 확인 팝업 요약 — 부작용 금지). */
    int selectRequestedCntByRelation(@Param("relationId") Long relationId);

    /**
     * 관계 해지 자동 취소 — 해당 관계의 REQUESTED 공유요청 전부 CANCELLED.
     * 스냅샷/상세행/번들은 무접촉(결정 3 — 수신 자료 존속). 영향행 수 반환.
     */
    int cancelShareReqByRelation(@Param("relationId") Long relationId, @Param("actionUserCd") String actionUserCd);
}
