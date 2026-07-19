package com.prafta.web.subcon.subcon02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.subcon.subcon02.application.command.DefectActionWriteCommand;
import com.prafta.web.subcon.subcon02.application.command.InspectAnswerWriteCommand;
import com.prafta.web.subcon.subcon02.result.ChkptSrcRaw;
import com.prafta.web.subcon.subcon02.result.ChkptTierRaw;
import com.prafta.web.subcon.subcon02.result.InspectItemSrcRaw;
import com.prafta.web.subcon.subcon02.result.ItemLinkPairRaw;
import com.prafta.web.subcon.subcon02.result.LinkDstRaw;

/**
 * 순회점검 구성 연동 + 점검 결과 통합 전용 매퍼(PRAFTA-SUBCON-T6).
 *
 * <p>T2 의 {@code Subcon02Mapper}(사업장/근무타입 미러)는 손대지 않고 본 매퍼로 분리한다
 * (T2 SQL 회귀 위험 차단). 전파/미러 값은 전부 DB 원본 행에서만 복제하며 사용자 입력을 경유하지 않는다.
 */
@Mapper
public interface ChkptLinkMapper {

    // =========================== 점검 연동 상태 전이(T6-02) ===========================

    /**
     * 점검 연동 실행 선점: CHKPT_LINK_STATUS NONE→ACTIVE.
     * 조건 = 사업장 링크 ACTIVE + 미러 사업장 존재 + 행위자 = SRC(제공측) 소속.
     * 영향행 수 반환(0=404 존재 비노출).
     */
    int enableChkptLinkPreempt(@Param("linkId") Long linkId, @Param("actorCmpnyCd") String actorCmpnyCd,
            @Param("actorUserCd") String actorUserCd);

    /**
     * 점검 연동 해제 선점: CHKPT_LINK_STATUS ACTIVE→NONE.
     * 조건 = 사업장 링크 ACTIVE + 행위자가 양측 중 어느 한쪽 소속. 영향행 수 반환(0=404).
     */
    int disableChkptLinkPreempt(@Param("linkId") Long linkId, @Param("actorCmpnyCd") String actorCmpnyCd,
            @Param("actorUserCd") String actorUserCd);

    /**
     * 사업장 링크 해지 훅용 점검 연동 상태 해제(당사자 조건 없음 — 링크 레벨 조건부 UPDATE 로 기증명된 경로 한정).
     * 영향행 수 반환.
     */
    int clearChkptLinkStatusBySystem(@Param("linkId") Long linkId, @Param("actionUserCd") String actionUserCd);

    /** 해당 SRC 사업장의 점검연동 ACTIVE 링크 목록(직속 미러 — 미러 확장/전파 재귀 단위). */
    List<LinkDstRaw> selectActiveChkptLinksBySrcSite(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd);

    // =========================== 미러 생성(T6-02, plan §4-1) ===========================

    /** 원본 사업장의 활성 점검대상 전량(복제 대상). */
    List<ChkptSrcRaw> selectSrcChkptList(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd);

    /** 원본 사업장의 활성 점검문항 전량(복제 대상). */
    List<InspectItemSrcRaw> selectSrcInspectItemList(@Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd);

    /** 미러 점검대상코드 신규 채번(DST 회사 시퀀스 — plan D1, 원본 코드 복사 금지). */
    String selectNewChkptCd(@Param("dstCmpnyCd") String dstCmpnyCd);

    /** 미러 점검문항코드 신규 채번(DST 회사 시퀀스 — chkLst02 selectNextInspectItemCd 규칙 동형). */
    String selectNewInspectItemCd(@Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("chkLstType") String chkLstType);

    /** 이미 존재하는 미러 점검대상코드(LINK_SRC 좌표 기준). 없으면 null — 미러 생성/전파의 멱등 판정. */
    String selectMirrorChkptCd(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("srcChkptCd") String srcChkptCd);

    /** 이미 존재하는 미러 점검문항코드(LINK_SRC 좌표 기준). 없으면 null. */
    String selectMirrorInspectItemCd(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("srcItemCd") String srcItemCd);

    // =========================== 재연동 재귀속(보안검토 M2) ===========================

    /** 독립화된 과거 미러 점검대상코드(PREV_LINK_SRC 좌표 기준). 없으면 null — 재연동 시 중복 INSERT 방지. */
    String selectOrphanMirrorChkptCd(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("srcChkptCd") String srcChkptCd);

    /** 독립화된 과거 미러 점검문항코드(PREV_LINK_SRC 좌표 기준). 없으면 null. */
    String selectOrphanMirrorInspectItemCd(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("srcItemCd") String srcItemCd);

    /** 점검대상 재귀속(PREV_LINK_SRC → LINK_SRC 복원). 영향행 수 반환. */
    int reattachMirrorChkpt(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("dstChkptCd") String dstChkptCd, @Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd, @Param("srcChkptCd") String srcChkptCd);

    /** 점검문항 재귀속(PREV_LINK_SRC → LINK_SRC 복원). 영향행 수 반환. */
    int reattachMirrorInspectItem(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("dstItemCd") String dstItemCd, @Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd, @Param("srcItemCd") String srcItemCd);

    /** 원본 문항의 점검구분(CHKLST_TYPE) — 미러 문항코드 채번 시퀀스 키가 점검구분에 종속되므로 DB 에서 읽는다. 없으면 null. */
    String selectInspectItemChkLstType(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("itemCd") String itemCd);

    /** 미러 점검대상 INSERT(MGMT_USER_CD=NULL — 담당자는 수신사가 지정, plan D8). 값은 원본 행에서만 복제. */
    int insertMirrorChkpt(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("chkptCd") String chkptCd, @Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd, @Param("srcChkptCd") String srcChkptCd);

    /** 미러 점검문항 INSERT(시행일/정렬순서/사용여부 원본 그대로 — T0 effective-dating 유지). */
    int insertMirrorInspectItem(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("inspectItemCd") String inspectItemCd, @Param("srcCmpnyCd") String srcCmpnyCd,
            @Param("srcSiteCd") String srcSiteCd, @Param("srcItemCd") String srcItemCd);

    /**
     * 미러 테넌트 문항 변경이력 INSERT(수신사 확인서 회색 게이팅이 HIST 기반이라 필수 — 엣지 1).
     * 저장 직후 미러 문항의 현재 상태를 스냅샷한다(INSERT_NO='SYSTEM').
     */
    void insertMirrorInspectItemHist(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("inspectItemCd") String inspectItemCd, @Param("chgType") String chgType);

    // =========================== 원본 변경 전파(T6-04) ===========================

    /** 점검대상 전파 — 원본 행 값으로 미러 UPDATE(MGMT_USER_CD 제외 — 수신사 운영 필드). 영향행 수 반환. */
    int propagateMirrorChkpt(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("srcChkptCd") String srcChkptCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd, @Param("dstChkptCd") String dstChkptCd);

    /** 점검문항 전파 — 원본 행 값으로 미러 UPDATE(명칭/정렬/시행일/사용여부 전량). 영향행 수 반환. */
    int propagateMirrorInspectItem(@Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd,
            @Param("srcItemCd") String srcItemCd, @Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd, @Param("dstItemCd") String dstItemCd);

    // =========================== 독립화(T6-02 해제 / T6-08 해지 훅) ===========================

    /** 미러 점검대상 LINK_SRC_* NULL 화(자체 점검대상 전환). 영향행 수 반환. */
    int clearChkptLinkSrc(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("updateNo") String updateNo);

    /** 미러 점검문항 LINK_SRC_* NULL 화(HIST 는 무접촉 — 이력 불변). 영향행 수 반환. */
    int clearInspectItemLinkSrc(@Param("dstCmpnyCd") String dstCmpnyCd, @Param("dstSiteCd") String dstSiteCd,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("updateNo") String updateNo);

    // =========================== write-through BFS(T6-05/06, plan §4-2) ===========================

    /** 기점 점검대상의 점검구분(문항 매핑표 조회 필터). 없으면 null. */
    String selectChkptChkLstType(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd);

    /**
     * [qa M-3] 부모(1홉 위) 점검대상 좌표 — 문항 좌표를 포함하지 않는다(점검대상 체인은 전 문항에 대해 불변).
     * 없으면 null.
     */
    ChkptTierRaw selectChkptParentTier(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd);

    /** [qa M-3] 자식(1홉 아래) 점검대상 좌표 목록 — 나를 LINK_SRC 로 가리키는 미러들. */
    List<ChkptTierRaw> selectChkptChildTiers(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd,
            @Param("chkptCd") String chkptCd);

    /**
     * [qa M-3] 사이트 쌍(수신 ← 원본) 문항 매핑표 — 체인 간선당 1회 조회로 전 문항의 좌표를 해석한다.
     * 반환 = (수신 문항코드, 원본 문항코드) 쌍 목록.
     */
    List<ItemLinkPairRaw> selectItemLinkPairs(@Param("dstCmpnyCd") String dstCmpnyCd,
            @Param("dstSiteCd") String dstSiteCd, @Param("chkLstType") String chkLstType,
            @Param("srcCmpnyCd") String srcCmpnyCd, @Param("srcSiteCd") String srcSiteCd);

    /**
     * 응답 복제(후행 덮어쓰기 — last-writer-wins) — 대응 좌표에 무조건 UPSERT 한다.
     * 선행 행이 있으면 값/사진/수행자 스냅샷을 최신으로 덮어쓴다. 영향행 수 반환.
     */
    int upsertAnswer(InspectAnswerWriteCommand command);

    /**
     * 불량조치 복제(후행 덮어쓰기 — last-writer-wins) — 대응 좌표에 무조건 UPSERT 한다.
     * 선행 조치가 있으면 내역/사진/조치자 스냅샷을 최신으로 덮어쓴다. 영향행 수 반환.
     */
    int upsertDefectAction(DefectActionWriteCommand command);
}
