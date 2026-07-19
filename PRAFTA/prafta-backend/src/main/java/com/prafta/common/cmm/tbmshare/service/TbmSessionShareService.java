package com.prafta.common.cmm.tbmshare.service;

import java.util.List;
import java.util.Map;

import com.prafta.common.cmm.tbmshare.result.AllowedCmpnyResult;
import com.prafta.common.cmm.tbmshare.result.SessionOwnerResult;
import com.prafta.common.cmm.tbmshare.result.SessionShareRow;
import com.prafta.common.cmm.tbmshare.result.ShareCandidateResult;
import com.prafta.common.cmm.tbmshare.result.TbmSessionAccess;

/**
 * TBM 연동 회사 지정 공통 검증 지점(PRAFTA-SUBCON-T5, 요청서 §3.2 "서버 공통 지점 1곳").
 *
 * <p>"세션 X 를 회사 C 가 볼 수 있는가 / 회사 C 소속이 입실할 수 있는가 / 회사 C 를 개설사 화면에
 * 뭐라고 표시하는가"를 서버 단일 지점에서 판정한다. web/tbm · app/tbm · app admin 3개 레이어가
 * 공통으로 주입하므로 {@code common.cmm} 에 둔다(선례: {@code common.cmm.worktime.WorktimeGateService}).
 *
 * <p>입실 범위(마스터 §4 소결정 ③): 참석자 소속 CMPNY_CD ∈ {개설사} ∪ SHARE 체인.
 * <b>출결 INSERT 를 수행하는 모든 경로(P1 앱 본인입실 / P2 웹 대리입실 / P3 앱 정규직 대리입실 /
 * P4 앱 일용직 QR)는 INSERT 직전에 반드시 {@link #assertEntryAllowed}를 통과해야 한다.</b>
 * 신규 입실 경로를 추가할 때 이 호출이 없으면 리뷰에서 반려된다.
 *
 * <p>조회 전용 메서드는 부작용이 없으며 자체 트랜잭션 경계를 두지 않는다(호출부 트랜잭션에 참여).
 */
public interface TbmSessionShareService {

    /**
     * 세션 조회 접근 판정. 개설사 본인이면 owner=true, SHARE 체인 소속이면 owner=false.
     * 둘 다 아니거나 세션이 없거나 삭제됐으면 TBM_404_030(존재 비노출).
     *
     * @param viewerSiteCd 개설사 본인일 때만 사용하는 사업장 스코프(null 이면 검사 생략).
     *                     앱 사용자는 자사 세션에 한해 기존 사업장 스코프를 유지한다(회귀 방지).
     *                     타사 세션은 회사 단위 지정이므로 사업장 검사를 하지 않는다(plan D4).
     * @param viewerUserTypeCd/viewerUserCd
     *                     F1 grandfather(지정 해제 후 <b>이미 입실한 참석자</b>의 조회·종료 허용) 판정용
     *                     본인 식별자. <b>사용자 단위</b>로 좁힌다(M4 — 회사 단위면 입실한 적 없는 같은 회사
     *                     직원까지 열람하게 된다). null 이면 grandfather 를 적용하지 않는다(웹 관리자 경로).
     */
    TbmSessionAccess assertViewable(String sessionCd, String viewerCmpnyCd, String viewerSiteCd,
            String viewerUserTypeCd, String viewerUserCd);

    /**
     * 입실 범위 강제(전 입실 경로 공통 통과 지점).
     * targetCmpnyCd ∈ {개설사} ∪ SHARE 체인(도달 가능) 이 아니면 TBM_403_060.
     *
     * @return 세션 소유 정보(개설사/사업장/상태) — 호출부가 개설사 기준으로 INSERT 가드를 건다.
     */
    SessionOwnerResult assertEntryAllowed(String sessionCd, String targetCmpnyCd);

    /** 지정/해제 권한 판정: actorCmpnyCd 가 개설사이거나 체인 내 회사여야 한다. 아니면 TBM_403_061. */
    SessionOwnerResult assertDesignatable(String sessionCd, String actorCmpnyCd);

    /**
     * N1: <b>클라이언트가 "대상 회사"로 지정할 수 있는 범위</b> 판정 — 개설사(자기 자신) 또는
     * <b>개설사 직하 1차 회사</b>만 허용한다. 그 외(2차 이하 체인 회사 포함)는 TBM_403_060.
     *
     * <p>{@link #assertEntryAllowed}(체인 전체 허용)를 클라 입력 검증에 그대로 쓰면, 회사코드를
     * 바꿔가며 호출했을 때 200/403 의 차이로 "그 회사가 이 세션 체인(2차 이하 포함)에 속하는지"를
     * 알아내는 <b>열거 오라클</b>이 된다(회사코드 공간이 짧아 전수 탐색이 현실적). 1차 회사는 이미
     * 개설사에게 공개된 정보({@link #selectAllowedCmpnyList})라 이 판정으로는 새 정보가 새지 않는다.
     *
     * <p>하위 체인으로의 확장은 서버가 수행한다({@link #selectEntryScopeCmpnyCds}) — 2차 이하 회사는
     * 클라가 지정할 수도, 존재를 알 수도 없다.
     *
     * <p>⚠ 입실 최종 게이트({@link #assertEntryAllowed})는 체인 전체 허용을 <b>그대로 유지</b>한다.
     * 서버가 도출한 실제 참석자 회사로 호출되기 때문이다(약화 금지).
     */
    SessionOwnerResult assertTier1Selectable(String sessionCd, String targetCmpnyCd);

    /**
     * 대상 회사 셀렉트 소스(F2) — <b>개설사(조회자 자신) + 개설사 직하 1차 회사</b>만.
     *
     * <p>2차 이하 회사코드/회사명/개수는 응답에 싣지 않는다(마스터 §1-3 인접 차수 가시성).
     * 1차 회사를 선택하면 하위 체인으로의 확장은 서버가 수행한다({@link #selectEntryScopeCmpnyCds}).
     */
    List<AllowedCmpnyResult> selectAllowedCmpnyList(String sessionCd, String viewerCmpnyCd);

    /**
     * F2: 선택된 대상 회사(개설사 또는 1차 회사)의 <b>입실/검색 범위 회사코드 집합</b>을 서버가 도출한다.
     * 개설사면 자기 자신 1건, 1차 회사면 그 회사 + 하위 재지정 체인 전체.
     * 클라이언트는 2차 이하 회사코드를 알 수 없으며, 출결행의 CMPNY_CD 는 이 집합 안에서 서버가 확정한다.
     */
    List<String> selectEntryScopeCmpnyCds(String sessionCd, String targetCmpnyCd);

    /**
     * F10: 세션코드 집합 → 개최 회사 라벨(내 SHARE 행의 DESIGNATED_BY 회사명) 배치 조회.
     * 목록 화면의 행별 라벨 조회(N+1)를 1회 조회로 대체한다. 자사 세션은 맵에 없다(라벨 없음).
     */
    Map<String, String> resolveHostLabels(List<String> sessionCds, String viewerCmpnyCd);

    /**
     * 개설사/체인 화면용 relabel 맵: 참석자 회사코드 → 표시 회사명(개설사 직하 1차 회사명).
     * 개설사 자신은 자기 회사명. 체인 밖 회사코드는 맵에 없다(호출자는 null-safe 처리).
     */
    Map<String, String> resolveTier1LabelMap(String sessionCd);

    /**
     * 참석자 소속사 화면용 라벨: 내가 보는 "개최 회사" = 내 SHARE 행의 DESIGNATED_BY 회사명.
     * 내가 개설사면 null(자사 세션이므로 배지/라벨 미표시). (하향 인접 차수 가시성)
     */
    String resolveHostLabelFor(String sessionCd, String viewerCmpnyCd);

    /* ===== 지정/해제(T5-03) ===== */

    /** 지정 후보 목록(관계 ACCEPTED − 개설사 − 이미 체인에 있는 회사). */
    List<ShareCandidateResult> selectDesignateCandidates(String sessionCd, String actorCmpnyCd);

    /** 지정 현황(내가 직접 지정한 회사만 + 하위 재지정 개사 수). */
    List<SessionShareRow> selectShareRows(String sessionCd, String actorCmpnyCd);

    /** 연동 회사 지정(신규 INSERT 또는 해제행 RESTORE). 가드는 plan §9-1. */
    void designate(String sessionCd, String actorCmpnyCd, String actorUserCd, String shareCmpnyCd);

    /** 연동 회사 지정 해제(자기가 지정한 행만) + 하위 재지정 캐스케이드. */
    void release(String sessionCd, String actorCmpnyCd, String actorUserCd, String shareCmpnyCd);

    /* ===== 관계 해지 훅(T1) ===== */

    /** 두 회사 관계 해지 시 해제될 지정 건수(하위 캐스케이드 포함). 부작용 없음. */
    int countSharesByRelation(String cmpnyCdA, String cmpnyCdB);

    /** 두 회사 관계 해지 시 지정 전량 해제 + 하위 캐스케이드(사유 RELATION_TERMINATED). */
    int releaseByRelation(String cmpnyCdA, String cmpnyCdB, String actionUserCd);
}
