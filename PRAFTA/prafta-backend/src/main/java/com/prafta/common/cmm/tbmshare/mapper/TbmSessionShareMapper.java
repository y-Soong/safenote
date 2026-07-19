package com.prafta.common.cmm.tbmshare.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.tbmshare.command.ShareDesignateCommand;
import com.prafta.common.cmm.tbmshare.command.ShareReleaseCommand;
import com.prafta.common.cmm.tbmshare.result.AllowedCmpnyResult;
import com.prafta.common.cmm.tbmshare.result.SessionHostLabelRow;
import com.prafta.common.cmm.tbmshare.result.SessionOwnerResult;
import com.prafta.common.cmm.tbmshare.result.SessionShareRow;
import com.prafta.common.cmm.tbmshare.result.ShareCandidateResult;
import com.prafta.common.cmm.tbmshare.result.ShareChainRow;
import com.prafta.common.cmm.tbmshare.result.ShareRefRow;
import com.prafta.common.cmm.tbmshare.result.ShareSlotResult;

/**
 * TBM 연동 회사 지정(TB_TBM_SESSION_SHARE) 매퍼 — PRAFTA-SUBCON-T5 공통 검증 지점.
 *
 * <p>SHARE 테이블에 대한 모든 SQL 을 본 매퍼 한 곳에 모은다(체인 재귀 CTE 중복 방지).
 * web/tbm·app/tbm·app admin 이 {@code TbmSessionShareService} 를 통해서만 접근한다.
 */
@Mapper
public interface TbmSessionShareMapper {

    /* ===== 세션 소유 ===== */

    /** 세션 소유/개설 정보(SESSION_CD 단독 — UX_TBM_SESSION_CD 전역 유일키 전제). */
    SessionOwnerResult selectSessionOwner(@Param("sessionCd") String sessionCd);

    /* ===== 체인 판정 ===== */

    /**
     * 도달성 판정: cmpnyCd 가 개설사로부터 DEL_YN='N' 간선만으로 도달 가능한가(1=가능).
     * <p>상향 walk(내 지정행 → 나를 지정한 회사 → ... → 개설사). depth 20 안전핀.
     */
    int countReachable(@Param("sessionCd") String sessionCd, @Param("cmpnyCd") String cmpnyCd);

    /** 개설사 하향 체인 전체(+1차 relabel 라벨). 개설사 화면 relabel 소스. */
    List<ShareChainRow> selectShareChain(
            @Param("sessionCd") String sessionCd, @Param("hostCmpnyCd") String hostCmpnyCd);

    /**
     * F1(grandfather): 이 회사 소속 참석자가 해당 세션에 이미 입실(DEL_YN='N')해 있는가.
     *
     * <p>지정 해제/관계 해지 후에도 <b>기존 참석자는 세션 조회·종료(서명 제출)를 계속할 수 있어야</b>
     * 한다(요청서 §3.1 · §5-2). 조회 게이트에서만 사용하며 신규 입실 게이트에서는 절대 쓰지 않는다.
     */
    int countMyAttendance(
            @Param("sessionCd") String sessionCd, @Param("cmpnyCd") String cmpnyCd,
            @Param("userTypeCd") String userTypeCd, @Param("userCd") String userCd);

    /**
     * F2: 개설사 직하 1차 회사 목록(코드+회사명). 대상 회사 셀렉트 소스.
     * <p>2차 이하 회사는 코드/회사명/개수 모두 응답에 실리지 않는다(마스터 §1-3 인접 차수 가시성).
     */
    List<AllowedCmpnyResult> selectTier1CmpnyList(
            @Param("sessionCd") String sessionCd, @Param("hostCmpnyCd") String hostCmpnyCd);

    /**
     * N1: 개설사 직하 1차 회사인가(DESIGNATED_BY_CMPNY_CD = 개설사, DEL_YN='N'). 1=1차 회사.
     *
     * <p>클라이언트가 "대상 회사"로 지정할 수 있는 범위를 1차로 제한해, 응답 코드 차이(200/403)로
     * 2차 이하 체인 멤버십을 알아내는 열거 오라클을 차단한다.
     */
    int countTier1Designation(@Param("sessionCd") String sessionCd,
            @Param("hostCmpnyCd") String hostCmpnyCd, @Param("shareCmpnyCd") String shareCmpnyCd);

    /** F10: 세션코드 집합 → 나를 지정한 직상위 회사명(하향 라벨) 배치 조회(N+1 제거). */
    List<SessionHostLabelRow> selectParentCmpnyNmBySessions(
            @Param("sessionCds") List<String> sessionCds, @Param("shareCmpnyCd") String shareCmpnyCd);

    /** 회사명 단건(개설사 자기 라벨용). 없으면 null. */
    String selectCmpnyNm(@Param("cmpnyCd") String cmpnyCd);

    /** 하향 라벨: 나를 지정한 직상위 회사명(내가 보는 "개최 회사"). 없으면 null. */
    String selectParentCmpnyNm(
            @Param("sessionCd") String sessionCd, @Param("shareCmpnyCd") String shareCmpnyCd);

    /* ===== 지정 현황/후보 ===== */

    /** 내가 직접 지정한 회사 목록(+하위 재지정 개사 수). */
    List<SessionShareRow> selectShareRows(
            @Param("sessionCd") String sessionCd, @Param("designatedByCmpnyCd") String designatedByCmpnyCd);

    /** 지정 후보: 행위자와 관계 ACCEPTED − 개설사 − 이미 체인에 있는 회사. */
    List<ShareCandidateResult> selectDesignateCandidates(
            @Param("sessionCd") String sessionCd
            , @Param("actorCmpnyCd") String actorCmpnyCd
            , @Param("hostCmpnyCd") String hostCmpnyCd);

    /** 관계 ACCEPTED 판정(방향 불문). T1 Subcon03Mapper.selectActiveRelationId 와 동일 술어. */
    int countActiveRelation(@Param("cmpnyCdA") String cmpnyCdA, @Param("cmpnyCdB") String cmpnyCdB);

    /* ===== 지정/해제 쓰기 ===== */

    /** 지정 슬롯(UK) 점유 행. 없으면 null(→INSERT), 있으면 DEL_YN 분기. */
    ShareSlotResult selectShareSlot(
            @Param("sessionCd") String sessionCd, @Param("shareCmpnyCd") String shareCmpnyCd);

    /** 신규 지정 INSERT. */
    int insertShare(ShareDesignateCommand command);

    /** 해제된 동일 슬롯 RESTORE(DEL_YN='Y' → 'N', 해제 감사 초기화). */
    int restoreShare(ShareDesignateCommand command);

    /** 해제(직접 + 캐스케이드 일괄). DEL_YN='N' 행만 갱신. */
    int releaseShares(ShareReleaseCommand command);

    /** 하향 후손 회사코드(자기 자신 포함). 캐스케이드 해제 대상 산출. */
    List<String> selectDescendantCmpnyCds(
            @Param("sessionCd") String sessionCd, @Param("rootCmpnyCd") String rootCmpnyCd);

    /* ===== 관계 해지 훅(T1) ===== */

    /** 두 회사 사이의 유효 지정(양방향). (DESIGNATED_BY=A AND SHARE=B) OR (DESIGNATED_BY=B AND SHARE=A). */
    List<ShareRefRow> selectSharesByCmpnyPair(
            @Param("cmpnyCdA") String cmpnyCdA, @Param("cmpnyCdB") String cmpnyCdB);
}
