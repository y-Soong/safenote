package com.prafta.web.subcon.subcon02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.subcon.subcon02.application.command.MirrorSiteInsertCommand;
import com.prafta.web.subcon.subcon02.application.command.SiteLinkInsertCommand;
import com.prafta.web.subcon.subcon02.application.command.SiteLinkProcessCommand;
import com.prafta.web.subcon.subcon02.result.LinkDstRaw;
import com.prafta.web.subcon.subcon02.result.MySiteResult;
import com.prafta.web.subcon.subcon02.result.RelationCmpnyResult;
import com.prafta.web.subcon.subcon02.result.SiteLinkRaw;
import com.prafta.web.subcon.subcon02.result.SiteLinkResult;
import com.prafta.web.subcon.subcon02.result.SiteLinkSrcRaw;
import com.prafta.web.subcon.subcon02.result.SiteSrcRaw;

@Mapper
public interface Subcon02Mapper {

    // =========================== 권한 게이트 ===========================

    /**
     * 메뉴 버튼 권한 보유 카운트(서버측 역할 게이트 — Subcon_02 메뉴, Subcon01 패턴 미러).
     * btnType 은 서비스 상수('SRCH'/'NEW'/'SAVE'/'DELT')만 전달하며, XML 에서 고정 컬럼으로 분기(동적 ${} 미사용).
     */
    int selectMenuButtonAuthCnt(@Param("cmpnyCd") String cmpnyCd, @Param("authCd") String authCd,
            @Param("menuDId") String menuDId, @Param("btnType") String btnType);

    // =========================== 조회(T2-02) ===========================

    /** 자사가 당사자(SRC 또는 DST)인 링크 전 상태 목록(목록=이력). 상한(limit) 필수. */
    List<SiteLinkResult> selectSiteLinkList(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("limit") int limit);

    /** 관계 ACCEPTED 상대 회사 목록(제안 후보 ① — 회사코드/회사명만). */
    List<RelationCmpnyResult> selectActiveRelationCmpnyList(@Param("gvCmpnyCd") String gvCmpnyCd);

    /** 내 활성 사업장 목록(제안 후보 ② — 미러 포함, linkYn 파생). */
    List<MySiteResult> selectMyActiveSiteList(@Param("gvCmpnyCd") String gvCmpnyCd);

    // =========================== 제안 가드(T2-02, §5-3) ===========================

    /** (cmpnyCdA, cmpnyCdB) 쌍의 ACCEPTED 관계ID(방향 불문). 미수립이면 null. */
    Long selectActiveRelationId(@Param("cmpnyCdA") String cmpnyCdA, @Param("cmpnyCdB") String cmpnyCdB);

    /** 제안측 소유 활성 사업장 존재 카운트(소유 검증 — §5-3 #1). */
    int selectMySiteActiveCnt(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * 사업장의 연동 출처(LINK_SRC_*) 단건 — 루프 가드 조상 순회용(§5-3 #4).
     * 행 미존재 시 null(순회 종료). 순회는 서버 데이터만 사용(클라 입력 불신).
     */
    SiteLinkSrcRaw selectSiteLinkSrc(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /**
     * 활성(PROPOSED/ACTIVE) 링크 카운트 — 같은 (SRC 사업장 → DST 회사) 1건 가드(§5-3 #5).
     * FOR UPDATE 직렬화 + UX_SITE_LINK_ACTIVE UNIQUE 가 DB 백스톱(T1 패턴 승계).
     */
    int selectActiveLinkCntForUpdate(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd, @Param("dstCmpnyCd") String dstCmpnyCd);

    /** 링크 INSERT(STATUS='PROPOSED' 고정, useGeneratedKeys 로 linkId 회수). */
    void insertSiteLink(SiteLinkInsertCommand command);

    // =========================== 상태 전이(T2-02/03/06, §4) ===========================

    /**
     * 수락 선점: PROPOSED→ACTIVE. 당사자 조건 DST_CMPNY_CD=actor + 관계 ACCEPTED 존속 검증.
     * 영향행 수 반환(0=404 — 동시 수락 레이스 차단).
     */
    int acceptSiteLinkPreempt(SiteLinkProcessCommand command);

    /** 거부: PROPOSED→REJECTED. 당사자 조건 DST_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int rejectSiteLink(SiteLinkProcessCommand command);

    /** 취소: PROPOSED→CANCELLED. 당사자 조건 SRC_CMPNY_CD=actor. 영향행 수 반환(0=404). */
    int cancelSiteLink(SiteLinkProcessCommand command);

    /** 해지: ACTIVE→TERMINATED. 당사자 조건 SRC 또는 DST=actor(양측 가능). 영향행 수 반환(0=404). */
    int terminateSiteLink(SiteLinkProcessCommand command);

    /**
     * 링크 원시행 단건(내부 전용 — 전이 성공 직후 미러 생성/독립화용.
     * 호출 전 조건부 UPDATE 성공으로 당사자성이 이미 증명된 경로에서만 사용).
     */
    SiteLinkRaw selectSiteLinkById(@Param("linkId") Long linkId);

    // =========================== 미러 생성(T2-03, §5-4) ===========================

    /** 수신측 미러 SITE_CD 채번(FNC_CMM_SEQ_NEXTVAL — baim01 시퀀스 재사용, DST 회사 기준). */
    String selectNewMirrorSiteCd(@Param("dstCmpnyCd") String dstCmpnyCd);

    /** 제공측 사업장 원시행(SITE_NO/SITE_NM — D4 보정·루트노드 명명용). 미존재 시 null. */
    SiteSrcRaw selectSiteSrcRaw(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd);

    /** DST 회사 내 SITE_NO 중복 카운트(D4 접미 보정 판정). */
    int selectSiteNoDupCnt(@Param("cmpnyCd") String cmpnyCd, @Param("siteNo") String siteNo);

    /** TB_SITE 복제(INSERT ... SELECT 원본행 — SITE_ADMIN_CD=NULL, LINK_SRC_* 세팅). 영향행 수 반환. */
    int insertMirrorSite(MirrorSiteInsertCommand command);

    /** 기본 부서노드 1개(루트 n1, SELF_ATTD_APPRV_YN='Y' — baim01 insertSiteNodeInfo 패턴 미러). */
    void insertMirrorRootNode(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("nodeNm") String nodeNm, @Param("insertNo") String insertNo);

    /** 전사역할(master/hr/safe/system) 사업장권한 자동부여(baim01 mergeMasterSiteAuthSet 패턴 미러). */
    void mergeMasterSiteAuthSet(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("insertNo") String insertNo);

    /** TB_SCH_MGMT 활성분 전량 복제(SCH_CD=원본 그대로 — D3, LINK_SRC_* 세팅). 복제 건수 반환. */
    int insertMirrorSchAll(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("insertNo") String insertNo);

    // =========================== 교대 정의 초기 복제(SHIFT-LINK-T2) ===========================

    /** TB_SHIFT_SCH_MGMT 활성분 전량 복제(SHIFT_CD=원본 그대로 — D3 동형, LINK_SRC_* 세팅). 복제 건수 반환. */
    int insertMirrorShiftAll(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("insertNo") String insertNo);

    /** 교대 패턴 회차 복제(부모 USE_YN='Y' 하위 전량 — SCH_CD 원본 그대로). 복제 건수 반환. */
    int insertMirrorShiftPtrnAll(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("insertNo") String insertNo);

    /** 교대 조 이름(메타) 복제(부모 USE_YN='Y' 하위 전량). 복제 건수 반환. */
    int insertMirrorShiftTeamMetaAll(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("insertNo") String insertNo);

    /** 교대 조 x 일차 배정표 복제(부모 USE_YN='Y' 하위 전량 — SCH_CD 원본 그대로). 복제 건수 반환. */
    int insertMirrorShiftAssignAll(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("insertNo") String insertNo);

    /** 링크 행에 채번된 DST_SITE_CD 기록(§5-4 #7). */
    int updateSiteLinkDstSite(@Param("linkId") Long linkId, @Param("dstSiteCd") String dstSiteCd,
            @Param("updateNo") String updateNo);

    // =========================== 독립화(T2-06, §5-6) ===========================

    /** 미러 사업장 LINK_SRC_* NULL 화(일반 사업장 전환 — 출처 일치 가드 포함). 영향행 수 반환. */
    int clearSiteLinkSrc(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("updateNo") String updateNo);

    /** 미러 사업장 소속 근무타입 전량 LINK_SRC_* NULL 화. 영향행 수 반환. */
    int clearSchLinkSrc(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("updateNo") String updateNo);

    /** 미러 사업장 소속 교대근무 타입 전량 LINK_SRC_* NULL 화(SHIFT-LINK-T5 — 출처 일치 가드). 영향행 수 반환. */
    int clearShiftLinkSrc(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("updateNo") String updateNo);

    /** 관계ID 산하 활성(PROPOSED/ACTIVE) 링크 목록(관계 해지 훅용 — T1 훅 계약 경로 한정). */
    List<SiteLinkRaw> selectActiveLinksByRelation(@Param("relationId") Long relationId);

    /** 관계 해지 자동 독립화: ACTIVE→TERMINATED(당사자 조건 없음 — 관계 레벨 기증명). 영향행 수 반환. */
    int terminateSiteLinkBySystem(@Param("linkId") Long linkId, @Param("actionUserCd") String actionUserCd);

    /** 관계 해지 자동 취소: PROPOSED→CANCELLED(잔존 제안 정리 — 고아 제안 방지). 영향행 수 반환. */
    int cancelSiteLinkBySystem(@Param("linkId") Long linkId, @Param("actionUserCd") String actionUserCd);

    // =========================== 전파(T2-05) ===========================

    /** 해당 (SRC 회사, SRC 사업장)의 ACTIVE 링크 목록(직속 미러 — 재귀 전파 단위). */
    List<LinkDstRaw> selectActiveLinksBySrcSite(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd);

    /**
     * 사업장 기본정보 전파: 원본 행 값으로 미러 UPDATE(전파 필드 = §5-5 잠금 필드,
     * SITE_NO(D4)·SITE_ADMIN_CD 제외). 값은 DB 원본에서만 복제(UPDATE_NO='SYSTEM'). 영향행 수 반환.
     */
    int propagateMirrorSite(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd);

    /**
     * 근무타입 전파: 원본 (사이트, SCH_CD) 행을 미러에 UPSERT(신규 추가 전파 = INSERT,
     * 변경 전파 = UPDATE. 미러 SCH_CD = 원본 SCH_CD — D3). 영향행 수 반환.
     */
    int propagateMirrorSch(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("schCd") String schCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd);

    /** 미러 테넌트 근무타입 이력 시퀀스(attd01 selectSchHistIdx 패턴 미러). */
    int selectMirrorSchHistIdx(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("schCd") String schCd);

    /** 미러 테넌트 TB_SCH_MGMT_HIST INSERT(전파 변경분 — D7, INSERT_NO='SYSTEM', 현재본 스냅샷). */
    void insertMirrorSchHist(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("schCd") String schCd, @Param("histIdx") int histIdx);

    // =========================== 교대 정의 전파(SHIFT-LINK-T3) ===========================

    /**
     * 교대 타입 정의 단건 전파(순수 INSERT — 교대 정의는 insert-only, 지시서 §2.1-2.
     * SHIFT_CD = 원본 그대로 — D3 동형, LINK_SRC_* 세팅, INSERT_NO/UPDATE_NO='SYSTEM'). 영향행 수 반환.
     */
    int propagateMirrorShift(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("shiftCd") String shiftCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd);

    /** 교대 패턴 회차 전파(단건 SHIFT_CD 하위 전량 — 순수 INSERT). 영향행 수 반환. */
    int propagateMirrorShiftPtrn(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("shiftCd") String shiftCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd);

    /** 교대 조 이름(메타) 전파(단건 SHIFT_CD 하위 전량 — 순수 INSERT). 영향행 수 반환. */
    int propagateMirrorShiftTeamMeta(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("shiftCd") String shiftCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd);

    /** 교대 조 x 일차 배정표 전파(단건 SHIFT_CD 하위 전량 — 순수 INSERT). 영향행 수 반환. */
    int propagateMirrorShiftAssign(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("shiftCd") String shiftCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd);
}
