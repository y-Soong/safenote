package com.prafta.web.subcon.subcon02.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.mapper.ChkptLinkMapper;
import com.prafta.web.subcon.subcon02.result.ChkptSrcRaw;
import com.prafta.web.subcon.subcon02.result.InspectItemSrcRaw;
import com.prafta.web.subcon.subcon02.result.LinkDstRaw;
import com.prafta.web.subcon.subcon02.result.SiteLinkRaw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 순회점검 구성 미러 생성/독립화 서비스(PRAFTA-SUBCON-T6-02, plan §4-1·§4-4).
 *
 * <p>미러 코드(CHKPT_CD / INSPECT_ITEM_CD)는 원본 코드를 복사하지 않고 <b>수신(DST) 회사 시퀀스로 신규 채번</b>한다
 * (plan D1). 두 코드 모두 회사 단위 시퀀스이고 미러 사업장에는 수신사 자체 항목이 공존하므로, 원본 코드를 그대로
 * 복사하면 수신사 시퀀스가 나중에 같은 값을 발급할 때 PK 가 충돌해 UPSERT 가 조용히 덮어쓴다(무경보 데이터 오염).
 * 원본 좌표는 LINK_SRC_* 로만 보관하고, 전파·write-through 는 이 매핑 컬럼으로만 해석한다.
 *
 * <p>복제는 (DST, LINK_SRC 좌표) 기준으로 <b>멱등</b>하다 — 이미 존재하는 미러는 재생성하지 않는다
 * (체인 하위 재귀 확장 시 중복 생성 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChkptLinkMirrorService {

    private final ChkptLinkMapper chkptLinkMapper;

    /** 체인 깊이 안전핀 — 초과 시 데이터 오염(루프)으로 간주하고 전체 롤백(T2 승계). */
    private static final int MAX_CHAIN_DEPTH = 20;

    /** 문항 변경이력 CHG_TYPE: 등록(chkLst02 상수와 동기 유지). */
    private static final String CHG_TYPE_REGIST = "01";

    /** 문항 변경이력 CHG_TYPE: 수정(재귀속으로 원본 값이 다시 덮인 경우). */
    private static final String CHG_TYPE_MODIFY = "02";

    /**
     * 점검 구성 미러 생성(문항 → 점검대상 순서) + 점검연동 ACTIVE 하위 체인 재귀 확장.
     * 호출자 트랜잭션에 참여한다(중간 실패 시 연동 실행 전체 롤백).
     */
    @Transactional
    public void mirrorChkptConfig(String srcCmpnyCd, String srcSiteCd, String dstCmpnyCd, String dstSiteCd) {
        mirrorChkptConfigInternal(srcCmpnyCd, srcSiteCd, dstCmpnyCd, dstSiteCd, 0);
    }

    /**
     * 점검 구성 독립화(plan §4-4) — 해당 링크 DST 사업장의 미러 점검대상·문항 LINK_SRC_* NULL 화.
     *
     * <p>이력(HIST)은 과거 사실이라 무접촉, 하위 체인도 무접촉(하위 미러의 LINK_SRC 는 수신사를 가리키므로 그대로 유효),
     * 기존 실적(응답·조치)은 전량 보존한다. 이후 전파는 매핑 부재로 자연 정지한다.
     */
    @Transactional
    public void independizeChkptMirror(SiteLinkRaw link, String actionUserCd) {
        if (link == null || link.dstSiteCd() == null) {
            return;
        }

        int chkptCleared = chkptLinkMapper.clearChkptLinkSrc(
                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);
        int itemCleared = chkptLinkMapper.clearInspectItemLinkSrc(
                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);

        log.info("점검 구성 독립화 - link={}, {}:{} 자체 구성 전환(점검대상 {}행, 문항 {}행)",
                link.linkId(), link.dstCmpnyCd(), link.dstSiteCd(), chkptCleared, itemCleared);
    }

    // =========================== private ===========================

    private void mirrorChkptConfigInternal(String srcCmpnyCd, String srcSiteCd,
            String dstCmpnyCd, String dstSiteCd, int depth) {

        if (depth > MAX_CHAIN_DEPTH) {
            log.error("점검 구성 미러 깊이 초과(데이터 오염 의심) - src={}:{}, depth={}", srcCmpnyCd, srcSiteCd, depth);
            throw new ApiException(SubconErrorCode.SUBCON_500_001);
        }

        // 1) 문항 먼저 복제(점검대상 조회 화면이 문항에 의존) — 활성분 전량.
        int itemCnt = 0;
        int itemReattachCnt = 0;
        List<InspectItemSrcRaw> srcItems = chkptLinkMapper.selectSrcInspectItemList(srcCmpnyCd, srcSiteCd);
        for (InspectItemSrcRaw src : srcItems) {
            String exists = chkptLinkMapper.selectMirrorInspectItemCd(
                    dstCmpnyCd, dstSiteCd, srcCmpnyCd, srcSiteCd, src.inspectItemCd());
            if (exists != null) {
                continue; // 이미 미러 존재(재귀 확장 경로) — 멱등 처리.
            }

            // 보안검토 M2: 과거 이 원본의 미러였다가 독립화된 행이 남아 있으면 재귀속한다(신규 INSERT 금지).
            //   해제는 행을 지우지 않고 LINK_SRC 만 NULL 로 만들기 때문에, 재연동마다 새로 INSERT 하면
            //   enable/disable 반복 횟수만큼 수신 테넌트에 점검문항이 중복 누적된다.
            if (reattachInspectItem(srcCmpnyCd, srcSiteCd, dstCmpnyCd, dstSiteCd, src.inspectItemCd())) {
                itemReattachCnt++;
                continue;
            }

            String newItemCd = chkptLinkMapper.selectNewInspectItemCd(dstCmpnyCd, src.chkLstType());
            chkptLinkMapper.insertMirrorInspectItem(
                    dstCmpnyCd, dstSiteCd, newItemCd, srcCmpnyCd, srcSiteCd, src.inspectItemCd());
            // 수신사 확인서 회색 게이팅이 HIST 기반이라 미러 테넌트 이력도 등록(01)으로 시작한다.
            chkptLinkMapper.insertMirrorInspectItemHist(dstCmpnyCd, dstSiteCd, newItemCd, CHG_TYPE_REGIST);
            itemCnt++;
        }

        // 2) 점검대상 복제 — MGMT_USER_CD 는 NULL 로 생성(수신사가 자기 직원 지정, plan D8).
        int chkptCnt = 0;
        int chkptReattachCnt = 0;
        List<ChkptSrcRaw> srcChkpts = chkptLinkMapper.selectSrcChkptList(srcCmpnyCd, srcSiteCd);
        for (ChkptSrcRaw src : srcChkpts) {
            String exists = chkptLinkMapper.selectMirrorChkptCd(
                    dstCmpnyCd, dstSiteCd, srcCmpnyCd, srcSiteCd, src.chkptCd());
            if (exists != null) {
                continue;
            }

            if (reattachChkpt(srcCmpnyCd, srcSiteCd, dstCmpnyCd, dstSiteCd, src.chkptCd())) {
                chkptReattachCnt++;
                continue;
            }

            String newChkptCd = chkptLinkMapper.selectNewChkptCd(dstCmpnyCd);
            chkptLinkMapper.insertMirrorChkpt(
                    dstCmpnyCd, dstSiteCd, newChkptCd, srcCmpnyCd, srcSiteCd, src.chkptCd());
            chkptCnt++;
        }

        log.info("점검 구성 미러 생성 - {}:{} -> {}:{}, 점검대상 신규 {}건/재귀속 {}건, 문항 신규 {}건/재귀속 {}건",
                srcCmpnyCd, srcSiteCd, dstCmpnyCd, dstSiteCd,
                chkptCnt, chkptReattachCnt, itemCnt, itemReattachCnt);

        // 3) 체인 하위 재귀 — 수신 미러가 다시 SRC 인 점검연동 ACTIVE 링크로 확장(n차 체인).
        List<LinkDstRaw> childLinks = chkptLinkMapper.selectActiveChkptLinksBySrcSite(dstCmpnyCd, dstSiteCd);
        for (LinkDstRaw child : childLinks) {
            mirrorChkptConfigInternal(dstCmpnyCd, dstSiteCd, child.dstCmpnyCd(), child.dstSiteCd(), depth + 1);
        }
    }

    /**
     * 과거 미러(독립화된 행) 재귀속 — 문항(보안검토 M2).
     *
     * <p>재귀속 후에는 독립 기간 동안 갈라졌을 수 있는 값을 원본 기준으로 재동기화하고(전파 SQL 재사용),
     * 수신 테넌트 이력에 수정(02)으로 남긴다(확인서 회색 게이팅 근거 유지).
     *
     * @return 재귀속했으면 true(신규 INSERT 불필요)
     */
    private boolean reattachInspectItem(String srcCmpnyCd, String srcSiteCd,
            String dstCmpnyCd, String dstSiteCd, String srcItemCd) {

        String orphanItemCd = chkptLinkMapper.selectOrphanMirrorInspectItemCd(
                dstCmpnyCd, dstSiteCd, srcCmpnyCd, srcSiteCd, srcItemCd);
        if (orphanItemCd == null) {
            return false;
        }

        int reattached = chkptLinkMapper.reattachMirrorInspectItem(
                dstCmpnyCd, dstSiteCd, orphanItemCd, srcCmpnyCd, srcSiteCd, srcItemCd);
        if (reattached <= 0) {
            log.warn("점검문항 재귀속 실패(경합 의심 - 신규 미러로 진행) - {}:{}:{} -> {}:{}:{}",
                    srcCmpnyCd, srcSiteCd, srcItemCd, dstCmpnyCd, dstSiteCd, orphanItemCd);
            return false;
        }

        chkptLinkMapper.propagateMirrorInspectItem(
                srcCmpnyCd, srcSiteCd, srcItemCd, dstCmpnyCd, dstSiteCd, orphanItemCd);
        chkptLinkMapper.insertMirrorInspectItemHist(dstCmpnyCd, dstSiteCd, orphanItemCd, CHG_TYPE_MODIFY);

        log.warn("점검문항 재귀속(과거 독립화 행 복구 - 중복 미러 생성 방지) - {}:{}:{} -> {}:{}:{}",
                srcCmpnyCd, srcSiteCd, srcItemCd, dstCmpnyCd, dstSiteCd, orphanItemCd);
        return true;
    }

    /** 과거 미러(독립화된 행) 재귀속 — 점검대상(보안검토 M2). 규칙은 문항과 동일(이력 테이블만 없음). */
    private boolean reattachChkpt(String srcCmpnyCd, String srcSiteCd,
            String dstCmpnyCd, String dstSiteCd, String srcChkptCd) {

        String orphanChkptCd = chkptLinkMapper.selectOrphanMirrorChkptCd(
                dstCmpnyCd, dstSiteCd, srcCmpnyCd, srcSiteCd, srcChkptCd);
        if (orphanChkptCd == null) {
            return false;
        }

        int reattached = chkptLinkMapper.reattachMirrorChkpt(
                dstCmpnyCd, dstSiteCd, orphanChkptCd, srcCmpnyCd, srcSiteCd, srcChkptCd);
        if (reattached <= 0) {
            log.warn("점검대상 재귀속 실패(경합 의심 - 신규 미러로 진행) - {}:{}:{} -> {}:{}:{}",
                    srcCmpnyCd, srcSiteCd, srcChkptCd, dstCmpnyCd, dstSiteCd, orphanChkptCd);
            return false;
        }

        chkptLinkMapper.propagateMirrorChkpt(
                srcCmpnyCd, srcSiteCd, srcChkptCd, dstCmpnyCd, dstSiteCd, orphanChkptCd);

        log.warn("점검대상 재귀속(과거 독립화 행 복구 - 중복 미러 생성 방지) - {}:{}:{} -> {}:{}:{}",
                srcCmpnyCd, srcSiteCd, srcChkptCd, dstCmpnyCd, dstSiteCd, orphanChkptCd);
        return true;
    }
}
