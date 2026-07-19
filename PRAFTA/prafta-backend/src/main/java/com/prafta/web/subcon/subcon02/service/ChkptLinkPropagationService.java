package com.prafta.web.subcon.subcon02.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.mapper.ChkptLinkMapper;
import com.prafta.web.subcon.subcon02.result.LinkDstRaw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 원본 점검대상/점검문항 변경의 재귀 전파 서비스(PRAFTA-SUBCON-T6-04).
 *
 * <p>T2 {@code SiteLinkPropagationService} 패턴 복제(T2 클래스 무수정). 전파 방식 =
 * 동기 전파 + 호출자 트랜잭션 참여(전파 실패 시 원본 저장 전체 롤백 — T2 D1 승계).
 * 전파 값은 DB 원본 행에서만 복제한다(사용자 입력 미경유 — 주입 면 차단, UPDATE_NO='SYSTEM').
 *
 * <p>재귀 단위 = tb_site_link 중 STATUS='ACTIVE' AND CHKPT_LINK_STATUS='ACTIVE' 행.
 * 점검 연동을 해제하면 매핑(LINK_SRC)이 사라져 전파가 자연 정지한다.
 *
 * <p>훅 호출부: {@code ChkLst01ServiceImpl.updateChkptList/deleteChkptList},
 * {@code ChkLst02ServiceImpl.updateChkptInspectItemList/deleteChkptInspectItemList/copyChkptInspectItemList}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChkptLinkPropagationService {

    private final ChkptLinkMapper chkptLinkMapper;

    /** 체인 깊이 안전핀 — 초과 시 데이터 오염(루프) 감지로 전체 롤백. */
    private static final int MAX_CHAIN_DEPTH = 20;

    /** 문항 변경이력 CHG_TYPE: 등록(미러가 새로 생긴 경우). */
    private static final String CHG_TYPE_REGIST = "01";

    /** 문항 변경이력 CHG_TYPE: 수정(독립화 행을 재귀속해 원본 값으로 되돌린 경우). */
    private static final String CHG_TYPE_MODIFY = "02";

    /**
     * [qa M-3] 사업장 링크 체인 캐시 생성 — 여러 행을 저장하는 호출부는 저장 1회당 한 번만 열어 전 행에 재사용한다.
     * 링크 위상(점검연동 ACTIVE 사업장 링크)은 그 트랜잭션 동안 불변이므로 행마다 재조회할 이유가 없다.
     */
    public ChkptLinkChainCache openChainCache() {
        return new ChkptLinkChainCache(chkptLinkMapper);
    }

    /**
     * 점검대상 변경 전파(명칭/비고/사용여부). 미러 부재 시 신규 채번 후 INSERT(원본에 신규 추가된 대상).
     * MGMT_USER_CD 는 수신사 운영 필드라 전파하지 않는다(plan D8).
     */
    @Transactional
    public void propagateChkpt(String cmpnyCd, String siteCd, String chkptCd) {
        propagateChkpt(cmpnyCd, siteCd, chkptCd, openChainCache());
    }

    /** 점검대상 변경 전파(링크 체인 캐시 재사용판 — 다건 저장 루프용). */
    @Transactional
    public void propagateChkpt(String cmpnyCd, String siteCd, String chkptCd, ChkptLinkChainCache cache) {
        if (isBlank(cmpnyCd) || isBlank(siteCd) || isBlank(chkptCd)) {
            return;
        }
        propagateChkptInternal(cmpnyCd, siteCd, chkptCd, 0, cache);
    }

    /**
     * 점검문항 변경 전파(명칭/정렬순서/시행일/사용여부).
     *
     * <p>시행일은 미러 측에서 GREATEST(원본 시행일, 미러 생성일=연동 실행일)로 보정된다(qa M-1, 전파 SQL 참조).
     * 수신사 기준 점검 의무는 연동 시점부터 발생하므로, 원본 시행일을 그대로 내리면 연동 이전 기간이
     * 수신사 확인서에서 미점검 의무(흰 셀)로 오표시된다.
     *
     * @param chgType 원본에 기록된 변경유형(01 등록 / 02 수정 / 03 사용중지 / 04 재사용).
     *                미러를 새로 만든 경우에는 '01'(등록)로 기록한다.
     */
    @Transactional
    public void propagateInspectItem(String cmpnyCd, String siteCd, String itemCd, String chgType) {
        propagateInspectItem(cmpnyCd, siteCd, itemCd, chgType, openChainCache());
    }

    /** 점검문항 변경 전파(링크 체인 캐시 재사용판 — 다건 저장 루프용, qa M-3). */
    @Transactional
    public void propagateInspectItem(String cmpnyCd, String siteCd, String itemCd, String chgType,
            ChkptLinkChainCache cache) {
        if (isBlank(cmpnyCd) || isBlank(siteCd) || isBlank(itemCd)) {
            return;
        }
        propagateInspectItemInternal(cmpnyCd, siteCd, itemCd, chgType, 0, cache);
    }

    // =========================== private ===========================

    private void propagateChkptInternal(String cmpnyCd, String siteCd, String chkptCd, int depth,
            ChkptLinkChainCache cache) {
        assertDepth(depth, cmpnyCd, siteCd);

        List<LinkDstRaw> links = cache.activeChkptLinks(cmpnyCd, siteCd);
        for (LinkDstRaw link : links) {

            String mirrorChkptCd = chkptLinkMapper.selectMirrorChkptCd(
                    link.dstCmpnyCd(), link.dstSiteCd(), cmpnyCd, siteCd, chkptCd);

            // 보안검토 M2: 과거 미러였다가 독립화된 행이 남아 있으면 신규 INSERT 대신 재귀속한다(중복 누적 방지).
            if (mirrorChkptCd == null) {
                mirrorChkptCd = reattachChkpt(link, cmpnyCd, siteCd, chkptCd);
            }

            if (mirrorChkptCd == null) {
                // 원본에 신규 추가된 점검대상 — 수신 회사 시퀀스로 채번 후 미러 생성.
                mirrorChkptCd = chkptLinkMapper.selectNewChkptCd(link.dstCmpnyCd());
                chkptLinkMapper.insertMirrorChkpt(
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorChkptCd, cmpnyCd, siteCd, chkptCd);
                log.info("점검대상 전파(신규 미러) - link={}, {}:{}:{} -> {}:{}:{}",
                        link.linkId(), cmpnyCd, siteCd, chkptCd,
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorChkptCd);
            } else {
                int affected = chkptLinkMapper.propagateMirrorChkpt(
                        cmpnyCd, siteCd, chkptCd, link.dstCmpnyCd(), link.dstSiteCd(), mirrorChkptCd);
                log.info("점검대상 전파 - link={}, {}:{}:{} -> {}:{}:{}, 영향행={}",
                        link.linkId(), cmpnyCd, siteCd, chkptCd,
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorChkptCd, affected);
            }

            // 하위 미러로 재귀(수신 미러가 다시 SRC 인 점검연동 ACTIVE 링크 — n차 체인).
            propagateChkptInternal(link.dstCmpnyCd(), link.dstSiteCd(), mirrorChkptCd, depth + 1, cache);
        }
    }

    private void propagateInspectItemInternal(String cmpnyCd, String siteCd, String itemCd,
            String chgType, int depth, ChkptLinkChainCache cache) {
        assertDepth(depth, cmpnyCd, siteCd);

        List<LinkDstRaw> links = cache.activeChkptLinks(cmpnyCd, siteCd);
        for (LinkDstRaw link : links) {

            String mirrorItemCd = chkptLinkMapper.selectMirrorInspectItemCd(
                    link.dstCmpnyCd(), link.dstSiteCd(), cmpnyCd, siteCd, itemCd);
            String mirrorChgType = chgType;

            // 보안검토 M2: 과거 미러였다가 독립화된 행 재귀속(신규 INSERT 대신) — 재연동 시 중복 누적 방지.
            boolean reattached = false;
            if (mirrorItemCd == null) {
                mirrorItemCd = reattachInspectItem(link, cmpnyCd, siteCd, itemCd);
                reattached = (mirrorItemCd != null);
                if (reattached) {
                    mirrorChgType = CHG_TYPE_MODIFY;
                }
            }

            // 보안검토 L1: 미러에 실제 반영(신규 생성/재귀속/UPDATE 영향행 1 이상)된 경우에만 HIST 를 남긴다.
            //   전파 0행인데 이력만 쌓이면 수신사 확인서의 회색 게이팅 근거(HIST)가 오염된다.
            boolean written;

            if (mirrorItemCd == null) {
                // 원본에 신규 추가된 문항(또는 가져오기로 생성된 문항) — 미러 신규 생성 = 등록 이력.
                //   채번 시퀀스 키가 점검구분에 종속되므로 원본 행에서 점검구분을 읽어온다(추측 금지).
                String chkLstType = chkptLinkMapper.selectInspectItemChkLstType(cmpnyCd, siteCd, itemCd);
                if (isBlank(chkLstType)) {
                    log.error("점검문항 전파 실패(원본 문항 미존재) - {}:{}:{}, link={}",
                            cmpnyCd, siteCd, itemCd, link.linkId());
                    throw new ApiException(SubconErrorCode.SUBCON_500_001);
                }
                mirrorItemCd = chkptLinkMapper.selectNewInspectItemCd(link.dstCmpnyCd(), chkLstType);
                chkptLinkMapper.insertMirrorInspectItem(
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd, cmpnyCd, siteCd, itemCd);
                mirrorChgType = CHG_TYPE_REGIST;
                written = true;
                log.info("점검문항 전파(신규 미러) - link={}, {}:{}:{} -> {}:{}:{}",
                        link.linkId(), cmpnyCd, siteCd, itemCd,
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd);
            } else {
                int affected = chkptLinkMapper.propagateMirrorInspectItem(
                        cmpnyCd, siteCd, itemCd, link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd);
                written = reattached || affected > 0;
                log.info("점검문항 전파 - link={}, {}:{}:{} -> {}:{}:{}, 재귀속={}, 영향행={}",
                        link.linkId(), cmpnyCd, siteCd, itemCd,
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd, reattached, affected);
            }

            // 미러 테넌트 이력 기록(수신사 확인서 회색 게이팅이 HIST 기반 — 엣지 1).
            if (written) {
                chkptLinkMapper.insertMirrorInspectItemHist(
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd, mirrorChgType);
            } else {
                log.warn("점검문항 전파 0행 - 미러 이력 미기록(게이팅 근거 오염 방지) - link={}, {}:{}:{} -> {}:{}:{}",
                        link.linkId(), cmpnyCd, siteCd, itemCd,
                        link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd);
            }

            propagateInspectItemInternal(link.dstCmpnyCd(), link.dstSiteCd(), mirrorItemCd, chgType, depth + 1, cache);
        }
    }

    /**
     * 독립화된 과거 미러 점검대상 재귀속(보안검토 M2). 재귀속했으면 그 미러 코드, 아니면 null.
     * 재귀속 직후 값은 아래 전파 UPDATE 가 원본 기준으로 재동기화한다.
     */
    private String reattachChkpt(LinkDstRaw link, String cmpnyCd, String siteCd, String chkptCd) {
        String orphanChkptCd = chkptLinkMapper.selectOrphanMirrorChkptCd(
                link.dstCmpnyCd(), link.dstSiteCd(), cmpnyCd, siteCd, chkptCd);
        if (orphanChkptCd == null) {
            return null;
        }
        int reattached = chkptLinkMapper.reattachMirrorChkpt(
                link.dstCmpnyCd(), link.dstSiteCd(), orphanChkptCd, cmpnyCd, siteCd, chkptCd);
        if (reattached <= 0) {
            return null;
        }
        log.warn("점검대상 재귀속(전파 경로 - 중복 미러 생성 방지) - link={}, {}:{}:{} -> {}:{}:{}",
                link.linkId(), cmpnyCd, siteCd, chkptCd,
                link.dstCmpnyCd(), link.dstSiteCd(), orphanChkptCd);
        return orphanChkptCd;
    }

    /** 독립화된 과거 미러 점검문항 재귀속(보안검토 M2). 재귀속했으면 그 미러 코드, 아니면 null. */
    private String reattachInspectItem(LinkDstRaw link, String cmpnyCd, String siteCd, String itemCd) {
        String orphanItemCd = chkptLinkMapper.selectOrphanMirrorInspectItemCd(
                link.dstCmpnyCd(), link.dstSiteCd(), cmpnyCd, siteCd, itemCd);
        if (orphanItemCd == null) {
            return null;
        }
        int reattached = chkptLinkMapper.reattachMirrorInspectItem(
                link.dstCmpnyCd(), link.dstSiteCd(), orphanItemCd, cmpnyCd, siteCd, itemCd);
        if (reattached <= 0) {
            return null;
        }
        log.warn("점검문항 재귀속(전파 경로 - 중복 미러 생성 방지) - link={}, {}:{}:{} -> {}:{}:{}",
                link.linkId(), cmpnyCd, siteCd, itemCd,
                link.dstCmpnyCd(), link.dstSiteCd(), orphanItemCd);
        return orphanItemCd;
    }

    /** 깊이 안전핀 — 초과 시 링크 데이터 오염(루프) 감지로 간주(로그 필수 + 전체 롤백). */
    private void assertDepth(int depth, String cmpnyCd, String siteCd) {
        if (depth > MAX_CHAIN_DEPTH) {
            log.error("점검 구성 전파 깊이 초과(데이터 오염 의심) - cmpnyCd={}, siteCd={}, depth={}", cmpnyCd, siteCd, depth);
            throw new ApiException(SubconErrorCode.SUBCON_500_001);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
