package com.prafta.common.cmm.tbmshare.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.common.cmm.tbmshare.command.ShareDesignateCommand;
import com.prafta.common.cmm.tbmshare.command.ShareReleaseCommand;
import com.prafta.common.cmm.tbmshare.mapper.TbmSessionShareMapper;
import com.prafta.common.cmm.tbmshare.result.AllowedCmpnyResult;
import com.prafta.common.cmm.tbmshare.result.SessionOwnerResult;
import com.prafta.common.cmm.tbmshare.result.SessionShareRow;
import com.prafta.common.cmm.tbmshare.result.ShareCandidateResult;
import com.prafta.common.cmm.tbmshare.result.SessionHostLabelRow;
import com.prafta.common.cmm.tbmshare.result.ShareChainRow;
import com.prafta.common.cmm.tbmshare.result.ShareRefRow;
import com.prafta.common.cmm.tbmshare.result.ShareSlotResult;
import com.prafta.common.cmm.tbmshare.result.TbmSessionAccess;
import com.prafta.common.cmm.tbmshare.service.TbmSessionShareService;
import com.prafta.common.error.tbm.TbmErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TBM 연동 회사 지정 공통 검증 지점 구현(PRAFTA-SUBCON-T5).
 *
 * <p>도달성(reachability)이 유일한 권위 판정이다(plan D1). 중간 지정이 해제되면 캐스케이드
 * 데이터가 없어도 하위는 자동으로 도달 불가가 된다 — 캐스케이드는 표시/감사용 보조 조치.
 *
 * <p>조회 메서드는 트랜잭션 어노테이션을 두지 않는다(읽기 전용, 호출부 트랜잭션 참여).
 * 쓰기 메서드는 호출부(@Transactional 서비스/훅)의 트랜잭션에 참여한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TbmSessionShareServiceImpl implements TbmSessionShareService {

    private final TbmSessionShareMapper tbmSessionShareMapper;

    /** 지정/해제 가능 세션 상태(plan D2 — 입실은 OPENED 에서만 일어나므로 그 이후 변경은 무의미). */
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_OPENED = "OPENED";

    /** 해제 사유구분. */
    private static final String REASON_MANUAL = "MANUAL";
    private static final String REASON_CASCADE = "CASCADE";
    private static final String REASON_RELATION_TERMINATED = "RELATION_TERMINATED";

    // ============================ 판정 ============================

    @Override
    public TbmSessionAccess assertViewable(String sessionCd, String viewerCmpnyCd, String viewerSiteCd,
            String viewerUserTypeCd, String viewerUserCd) {
        SessionOwnerResult session = loadSession(sessionCd);

        boolean owner = session.hostCmpnyCd().equals(viewerCmpnyCd);
        if (owner) {
            // 자사 세션: 기존 사업장 스코프를 유지한다(앱 회귀 방지). viewerSiteCd 미지정이면 검사 생략.
            if (StringUtils.hasText(viewerSiteCd) && !viewerSiteCd.equals(session.siteCd())) {
                log.info("[tbmshare] 자사 세션 사업장 스코프 불일치 - sessionCd={}", sessionCd);
                throw new ApiException(TbmErrorCode.TBM_404_030);
            }
            return new TbmSessionAccess(session, true);
        }

        // 타사 세션: 지정 체인에 내 회사가 도달 가능해야 한다(회사 단위 — 사업장 검사 없음, plan D4).
        if (isReachable(sessionCd, viewerCmpnyCd)) {
            return new TbmSessionAccess(session, false);
        }

        // F1(grandfather): 도달 불가여도 <b>이미 입실한 참석자</b>가 있으면 조회를 허용한다.
        //   지정 해제/관계 해지는 "신규 입실만 차단"이며 기존 참석자는 유지된다(요청서 §3.1 · §5-2).
        //   이 분기가 없으면 해제 즉시 기존 참석자가 세션 조회·종료(서명 제출)에서 404 를 맞고
        //   강제 미이수 처리되어 법정 교육 이수 기록이 소실된다.
        //   ⚠ 조회 전용 완화다. 신규 입실(assertEntryAllowed)은 엄격한 도달성 판정을 그대로 유지한다.
        //   M4: 회사 단위가 아니라 <b>사용자 단위</b>로 좁힌다(입실한 적 없는 같은 회사 직원은 열람 불가).
        if (StringUtils.hasText(viewerUserTypeCd) && StringUtils.hasText(viewerUserCd)
                && tbmSessionShareMapper.countMyAttendance(
                        sessionCd, viewerCmpnyCd, viewerUserTypeCd, viewerUserCd) > 0) {
            log.info("[tbmshare] 지정 해제 후 기존 참석자 조회 허용(grandfather) - sessionCd={}", sessionCd);
            return new TbmSessionAccess(session, false);
        }

        // 존재 비노출: 미지정 회사에는 세션의 존재 자체를 알리지 않는다.
        log.info("[tbmshare] 세션 접근 불가(지정 체인 밖) - sessionCd={}", sessionCd);
        throw new ApiException(TbmErrorCode.TBM_404_030);
    }

    @Override
    public SessionOwnerResult assertEntryAllowed(String sessionCd, String targetCmpnyCd) {
        if (!StringUtils.hasText(targetCmpnyCd)) {
            throw new ApiException(TbmErrorCode.TBM_400_060);
        }
        SessionOwnerResult session = loadSession(sessionCd);

        if (session.hostCmpnyCd().equals(targetCmpnyCd)) {
            return session;    // 개설사 본인은 항상 허용
        }
        if (!isReachable(sessionCd, targetCmpnyCd)) {
            log.warn("[tbmshare] 입실 범위 밖 차단 - sessionCd={}, targetCmpnyCd={}", sessionCd, targetCmpnyCd);
            throw new ApiException(TbmErrorCode.TBM_403_060);
        }
        return session;
    }

    /**
     * N1: 클라이언트가 지정 가능한 "대상 회사" = 개설사 자신 또는 개설사 직하 1차 회사.
     *
     * <p>체인 전체를 통과시키면 회사코드를 바꿔가며 호출해 200/403 차이로 2차 이하 체인 멤버십을
     * 알아내는 열거 오라클이 된다(그리고 200 이면 그 회사 후보의 PII 까지 열람된다).
     * 1차 회사는 이미 개설사에게 노출된 정보이므로 이 판정으로는 새 정보가 새지 않는다.
     */
    @Override
    public SessionOwnerResult assertTier1Selectable(String sessionCd, String targetCmpnyCd) {
        if (!StringUtils.hasText(targetCmpnyCd)) {
            throw new ApiException(TbmErrorCode.TBM_400_060);
        }
        SessionOwnerResult session = loadSession(sessionCd);

        if (session.hostCmpnyCd().equals(targetCmpnyCd)) {
            return session;     // 자사(개설사) 대상
        }
        int tier1 = tbmSessionShareMapper.countTier1Designation(
                sessionCd, session.hostCmpnyCd(), targetCmpnyCd);
        if (tier1 <= 0) {
            log.warn("[tbmshare] 대상 회사 선택 불가(1차 연동 회사 아님) - sessionCd={}", sessionCd);
            throw new ApiException(TbmErrorCode.TBM_403_060);
        }
        return session;
    }

    @Override
    public SessionOwnerResult assertDesignatable(String sessionCd, String actorCmpnyCd) {
        SessionOwnerResult session = loadSession(sessionCd);

        if (session.hostCmpnyCd().equals(actorCmpnyCd)) {
            return session;
        }
        if (!isReachable(sessionCd, actorCmpnyCd)) {
            log.warn("[tbmshare] 연동 회사 지정 권한 없음 - sessionCd={}, actorCmpnyCd={}", sessionCd, actorCmpnyCd);
            throw new ApiException(TbmErrorCode.TBM_403_061);
        }
        return session;
    }

    // ============================ 라벨/목록 ============================

    /**
     * F2: 대상 회사 셀렉트 소스 = <b>개설사 + 개설사 직하 1차 회사</b>만.
     *
     * <p>체인 전체를 내리면 2차 이하 회사코드와 그 개수가 응답에 그대로 실린다(회사명만 1차로 접어도
     * 코드/항목수로 하위 구조가 드러나고, UI 에도 동일 회사명이 중복 표시되어 선택 불가). 마스터
     * §1-3(인접 차수 가시성)이 우선하므로 셀렉트 소스를 1차로 축약하고, 1차 회사를 선택하면
     * <b>서버가 그 하위 체인 전체로 후보 검색 범위를 확장</b>한다({@link #selectEntryScopeCmpnyCds}).
     * 클라이언트는 2차 이하 회사코드를 끝까지 알 수 없다.
     */
    @Override
    public List<AllowedCmpnyResult> selectAllowedCmpnyList(String sessionCd, String viewerCmpnyCd) {
        SessionOwnerResult session = loadSession(sessionCd);

        List<AllowedCmpnyResult> list = new ArrayList<>();

        // 첫 항목 = 조회자 자신(웹 셀렉트 기본값 = 자사).
        String selfNm = tbmSessionShareMapper.selectCmpnyNm(viewerCmpnyCd);
        list.add(new AllowedCmpnyResult(viewerCmpnyCd, selfNm != null ? selfNm : ""));

        // 개설사 직하 1차 회사만(2차 이하 비노출).
        List<AllowedCmpnyResult> tier1 =
                tbmSessionShareMapper.selectTier1CmpnyList(sessionCd, session.hostCmpnyCd());
        if (tier1 != null) {
            for (AllowedCmpnyResult row : tier1) {
                if (row.cmpnyCd().equals(viewerCmpnyCd)) {
                    continue;   // 자신 중복 방지
                }
                list.add(row);
            }
        }
        return list;
    }

    @Override
    public List<String> selectEntryScopeCmpnyCds(String sessionCd, String targetCmpnyCd) {
        SessionOwnerResult session = loadSession(sessionCd);

        // 개설사 자신을 고르면 확장 없음(자사 단독).
        if (session.hostCmpnyCd().equals(targetCmpnyCd)) {
            return List.of(targetCmpnyCd);
        }

        // 1차 회사를 고르면 그 하위 재지정 체인 전체가 검색/입실 범위가 된다(서버 도출 — 클라 미노출).
        List<String> scope = tbmSessionShareMapper.selectDescendantCmpnyCds(sessionCd, targetCmpnyCd);
        if (scope == null || scope.isEmpty()) {
            return List.of(targetCmpnyCd);
        }
        return scope;
    }

    @Override
    public Map<String, String> resolveHostLabels(List<String> sessionCds, String viewerCmpnyCd) {
        Map<String, String> map = new LinkedHashMap<>();
        if (sessionCds == null || sessionCds.isEmpty()) {
            return map;
        }
        List<SessionHostLabelRow> rows =
                tbmSessionShareMapper.selectParentCmpnyNmBySessions(sessionCds, viewerCmpnyCd);
        if (rows != null) {
            for (SessionHostLabelRow row : rows) {
                map.put(row.sessionCd(), row.hostCmpnyNm());
            }
        }
        return map;
    }

    @Override
    public Map<String, String> resolveTier1LabelMap(String sessionCd) {
        SessionOwnerResult session = loadSession(sessionCd);

        Map<String, String> map = new LinkedHashMap<>();
        // 개설사 자신은 자기 회사명(체인 조회로는 안 나오므로 별도 주입).
        String hostNm = tbmSessionShareMapper.selectCmpnyNm(session.hostCmpnyCd());
        map.put(session.hostCmpnyCd(), hostNm != null ? hostNm : "");

        List<ShareChainRow> chain = tbmSessionShareMapper.selectShareChain(sessionCd, session.hostCmpnyCd());
        if (chain != null) {
            for (ShareChainRow row : chain) {
                // 2차 이하 회사는 자기를 낳은 1차 회사명으로 접힌다(마스터 §1-3). 2차 이하 회사명은 어떤
                // 응답에도 실리지 않는다.
                map.put(row.shareCmpnyCd(), row.tier1CmpnyNm());
            }
        }
        return map;
    }

    @Override
    public String resolveHostLabelFor(String sessionCd, String viewerCmpnyCd) {
        SessionOwnerResult session = loadSession(sessionCd);
        if (session.hostCmpnyCd().equals(viewerCmpnyCd)) {
            return null;    // 자사 세션 — 개최사 라벨 없음(앱 배지 미표시)
        }
        return tbmSessionShareMapper.selectParentCmpnyNm(sessionCd, viewerCmpnyCd);
    }

    // ============================ 지정/해제 ============================

    @Override
    public List<ShareCandidateResult> selectDesignateCandidates(String sessionCd, String actorCmpnyCd) {
        SessionOwnerResult session = assertDesignatable(sessionCd, actorCmpnyCd);
        List<ShareCandidateResult> list = tbmSessionShareMapper.selectDesignateCandidates(
                sessionCd, actorCmpnyCd, session.hostCmpnyCd());
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public List<SessionShareRow> selectShareRows(String sessionCd, String actorCmpnyCd) {
        List<SessionShareRow> list = tbmSessionShareMapper.selectShareRows(sessionCd, actorCmpnyCd);
        return list != null ? list : new ArrayList<>();
    }

    @Override
    public void designate(String sessionCd, String actorCmpnyCd, String actorUserCd, String shareCmpnyCd) {
        if (!StringUtils.hasText(shareCmpnyCd)) {
            throw new ApiException(TbmErrorCode.TBM_400_060);
        }

        // 1. 지정 권한(개설사 또는 체인 내 회사).
        SessionOwnerResult session = assertDesignatable(sessionCd, actorCmpnyCd);

        // 2. 상태 가드: DRAFT/OPENED 에서만 지정 변경(plan D2).
        assertChangeableStatus(session);

        // 3. 자기 회사/개설사 지정 거부.
        if (shareCmpnyCd.equals(actorCmpnyCd) || shareCmpnyCd.equals(session.hostCmpnyCd())) {
            log.warn("[tbmshare] 지정 대상 부적합(자사/개설사) - sessionCd={}", sessionCd);
            throw new ApiException(TbmErrorCode.TBM_400_060);
        }

        // 4. 관계 ACCEPTED 검증(행위자 회사 ↔ 지정 대상, 방향 불문).
        if (tbmSessionShareMapper.countActiveRelation(actorCmpnyCd, shareCmpnyCd) <= 0) {
            log.warn("[tbmshare] 지정 대상과 연동 관계 없음 - sessionCd={}", sessionCd);
            throw new ApiException(TbmErrorCode.TBM_409_062);
        }

        // 5. 슬롯 분기: 없으면 INSERT, DEL_YN='N' 이면 중복 지정, DEL_YN='Y' 면 RESTORE.
        ShareDesignateCommand command = new ShareDesignateCommand(
                sessionCd, session.hostCmpnyCd(), shareCmpnyCd, actorCmpnyCd, actorUserCd);

        ShareSlotResult slot = tbmSessionShareMapper.selectShareSlot(sessionCd, shareCmpnyCd);
        if (slot == null) {
            try {
                tbmSessionShareMapper.insertShare(command);
            } catch (DuplicateKeyException e) {
                // 조회~INSERT 사이 동시 지정 경합(UK 백스톱).
                log.warn("[tbmshare] 지정 UNIQUE 충돌(동시 지정) - sessionCd={}", sessionCd);
                throw new ApiException(TbmErrorCode.TBM_409_061);
            }
        } else if ("N".equals(slot.delYn())) {
            log.info("[tbmshare] 이미 지정된 회사 - sessionCd={}", sessionCd);
            throw new ApiException(TbmErrorCode.TBM_409_061);
        } else {
            int affected = tbmSessionShareMapper.restoreShare(command);
            if (affected == 0) {
                // 경합으로 슬롯 상태 변경됨(이미 다른 트랜잭션이 재지정).
                log.warn("[tbmshare] 지정 RESTORE 경합 - sessionCd={}", sessionCd);
                throw new ApiException(TbmErrorCode.TBM_409_061);
            }
        }

        log.info("TBM 연동 회사 지정 완료 - sessionCd={}, shareCmpnyCd={}, byCmpnyCd={}, byUserCd={}",
                sessionCd, shareCmpnyCd, actorCmpnyCd, actorUserCd);
    }

    @Override
    public void release(String sessionCd, String actorCmpnyCd, String actorUserCd, String shareCmpnyCd) {
        if (!StringUtils.hasText(shareCmpnyCd)) {
            throw new ApiException(TbmErrorCode.TBM_400_060);
        }

        SessionOwnerResult session = assertDesignatable(sessionCd, actorCmpnyCd);
        assertChangeableStatus(session);

        // 내가 지정한 행만 해제할 수 있다(아니면 존재 비노출 404).
        ShareSlotResult slot = tbmSessionShareMapper.selectShareSlot(sessionCd, shareCmpnyCd);
        if (slot == null || !"N".equals(slot.delYn())) {
            throw new ApiException(TbmErrorCode.TBM_404_060);
        }
        List<SessionShareRow> mine = tbmSessionShareMapper.selectShareRows(sessionCd, actorCmpnyCd);
        boolean isMine = mine != null && mine.stream().anyMatch(r -> shareCmpnyCd.equals(r.cmpnyCd()));
        if (!isMine) {
            log.warn("[tbmshare] 해제 대상이 내 지정분이 아님 - sessionCd={}", sessionCd);
            throw new ApiException(TbmErrorCode.TBM_404_060);
        }

        int released = releaseWithCascade(sessionCd, shareCmpnyCd, REASON_MANUAL, actorCmpnyCd, actorUserCd);

        log.info("TBM 연동 회사 지정 해제 완료 - sessionCd={}, shareCmpnyCd={}, 해제(하위 포함)={}건",
                sessionCd, shareCmpnyCd, released);
    }

    // ============================ 관계 해지 훅 ============================

    @Override
    public int countSharesByRelation(String cmpnyCdA, String cmpnyCdB) {
        List<ShareRefRow> refs = tbmSessionShareMapper.selectSharesByCmpnyPair(cmpnyCdA, cmpnyCdB);
        if (refs == null || refs.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ShareRefRow ref : refs) {
            List<String> targets = tbmSessionShareMapper.selectDescendantCmpnyCds(
                    ref.sessionCd(), ref.shareCmpnyCd());
            total += (targets != null && !targets.isEmpty()) ? targets.size() : 1;
        }
        return total;
    }

    @Override
    public int releaseByRelation(String cmpnyCdA, String cmpnyCdB, String actionUserCd) {
        List<ShareRefRow> refs = tbmSessionShareMapper.selectSharesByCmpnyPair(cmpnyCdA, cmpnyCdB);
        if (refs == null || refs.isEmpty()) {
            return 0;
        }

        int released = 0;
        for (ShareRefRow ref : refs) {
            // 지정된 회사와 그 하위 재지정 전부 해제. 참석행은 건드리지 않는다(기존 참석자 유지).
            released += releaseWithCascade(
                    ref.sessionCd(), ref.shareCmpnyCd(), REASON_RELATION_TERMINATED, null, actionUserCd);
        }

        log.info("TBM 연동 회사 지정 자동 해제(관계 해지) - cmpnyA={}, cmpnyB={}, 해제={}건",
                cmpnyCdA, cmpnyCdB, released);
        return released;
    }

    // ============================ 내부 헬퍼 ============================

    /** 세션 소유 조회(미존재/삭제 → 404 존재 비노출). */
    private SessionOwnerResult loadSession(String sessionCd) {
        if (!StringUtils.hasText(sessionCd)) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }
        SessionOwnerResult session = tbmSessionShareMapper.selectSessionOwner(sessionCd);
        if (session == null || "Y".equals(session.delYn())) {
            throw new ApiException(TbmErrorCode.TBM_404_030);
        }
        return session;
    }

    /** 도달성 판정(개설사 → ... → cmpnyCd, DEL_YN='N' 간선만). */
    private boolean isReachable(String sessionCd, String cmpnyCd) {
        if (!StringUtils.hasText(cmpnyCd)) {
            return false;
        }
        return tbmSessionShareMapper.countReachable(sessionCd, cmpnyCd) > 0;
    }

    /** 지정/해제 가능 상태(DRAFT/OPENED) 가드. */
    private void assertChangeableStatus(SessionOwnerResult session) {
        if (!STATUS_DRAFT.equals(session.statusCd()) && !STATUS_OPENED.equals(session.statusCd())) {
            log.warn("[tbmshare] 연동 회사 변경 불가 상태 - sessionCd={}, status={}",
                    session.sessionCd(), session.statusCd());
            throw new ApiException(TbmErrorCode.TBM_409_063);
        }
    }

    /**
     * 지정 해제 + 하위 재지정 캐스케이드(동일 트랜잭션).
     * 도달성 판정이 이미 하위를 차단하므로 기능상 필수는 아니나, 화면 표시(지정 목록)·감사·재지정
     * UK 충돌 방지를 위해 물리적으로도 하위를 해제한다(plan §5-4).
     */
    private int releaseWithCascade(String sessionCd, String rootCmpnyCd, String reasonCd,
            String releasedByCmpnyCd, String releasedByUserCd) {

        List<String> targets = tbmSessionShareMapper.selectDescendantCmpnyCds(sessionCd, rootCmpnyCd);
        if (targets == null || targets.isEmpty()) {
            return 0;
        }

        // 루트(직접 해제)와 하위(캐스케이드)의 사유를 구분해 감사 흔적을 남긴다.
        List<String> descendants = new ArrayList<>(targets);
        descendants.remove(rootCmpnyCd);

        int affected = tbmSessionShareMapper.releaseShares(new ShareReleaseCommand(
                sessionCd, List.of(rootCmpnyCd), reasonCd, releasedByCmpnyCd, releasedByUserCd));

        if (!descendants.isEmpty()) {
            affected += tbmSessionShareMapper.releaseShares(new ShareReleaseCommand(
                    sessionCd, descendants,
                    REASON_RELATION_TERMINATED.equals(reasonCd) ? reasonCd : REASON_CASCADE,
                    releasedByCmpnyCd, releasedByUserCd));
        }
        return affected;
    }
}
