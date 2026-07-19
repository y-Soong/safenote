package com.prafta.web.subcon.subcon02.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.prafta.web.subcon.subcon01.result.TerminationImpactItem;
import com.prafta.web.subcon.subcon01.service.RelationTerminationHandler;
import com.prafta.web.subcon.subcon02.mapper.Subcon02Mapper;
import com.prafta.web.subcon.subcon02.result.SiteLinkRaw;
import com.prafta.web.subcon.subcon02.service.SiteLinkTerminationListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관계(T1) 해지 시 산하 사업장 연동 자동 종결 핸들러(PRAFTA-SUBCON-T2-06).
 *
 * <p>T1 {@link RelationTerminationHandler} 계약 구현체 — 빈 등록만으로
 * Subcon01ServiceImpl 의 ObjectProvider 주입에 수집된다(T1 코드 무수정).
 *
 * <ul>
 *   <li>summarize: 관계 산하 활성 링크 건수 요약(해지 확인 팝업 표시 — 부작용 금지).</li>
 *   <li>onTerminated: ACTIVE 링크 전부 해지+독립화, PROPOSED 링크 전부 자동 취소
 *       (고아 제안 방지 — 수락 선점의 관계 ACCEPTED 존속 검증과 이중 방어).
 *       동일 트랜잭션 — 예외 시 관계 해지 전체 롤백(T1 훅 계약).</li>
 *   <li>불변식: TB_CMPNY 상태에는 절대 작용하지 않는다(plan 정오표 2).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteLinkTerminationHandler implements RelationTerminationHandler {

    private final Subcon02Mapper subcon02Mapper;

    /**
     * PRAFTA-SUBCON-T6-08: 사업장 링크 해지 시 산하 연동 자동 독립화 리스너(구현체 0개면 no-op).
     * ObjectProvider 주입이라 빈이 없어도 기동/동작에 지장이 없다.
     */
    private final ObjectProvider<SiteLinkTerminationListener> siteLinkTerminationListeners;

    @Override
    public List<TerminationImpactItem> summarize(long relationId, String cmpnyCdA, String cmpnyCdB) {
        List<SiteLinkRaw> links = subcon02Mapper.selectActiveLinksByRelation(relationId);

        long activeCnt = links.stream().filter(l -> "ACTIVE".equals(l.status())).count();
        long proposedCnt = links.stream().filter(l -> "PROPOSED".equals(l.status())).count();

        List<TerminationImpactItem> impacts = new ArrayList<>();
        if (activeCnt > 0) {
            impacts.add(new TerminationImpactItem("SITE_LINK", "사업장 연동 해지(독립화) 예정", (int) activeCnt));
        }
        if (proposedCnt > 0) {
            impacts.add(new TerminationImpactItem("SITE_LINK_PROPOSED", "진행 중 사업장 연동 제안 자동취소 예정", (int) proposedCnt));
        }
        return impacts;
    }

    @Override
    public void onTerminated(long relationId, String cmpnyCdA, String cmpnyCdB, String actionUserCd) {
        List<SiteLinkRaw> links = subcon02Mapper.selectActiveLinksByRelation(relationId);
        if (links.isEmpty()) {
            return;
        }

        int terminated = 0;
        int cancelled = 0;
        for (SiteLinkRaw link : links) {
            if ("ACTIVE".equals(link.status())) {
                // 해지 = 독립화: 링크 TERMINATED + 수신 미러 LINK_SRC_* NULL 화.
                // 하위 체인 링크는 무접촉(수신사가 새 루트 — §5-6 #3).
                if (subcon02Mapper.terminateSiteLinkBySystem(link.linkId(), actionUserCd) > 0) {
                    terminated++;
                    if (link.dstSiteCd() != null) {
                        subcon02Mapper.clearSiteLinkSrc(
                                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);
                        subcon02Mapper.clearSchLinkSrc(
                                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);
                        // PRAFTA-SUBCON-T6-08: 산하 연동(점검 구성) 자동 독립화 훅 — 동일 트랜잭션.
                        siteLinkTerminationListeners.orderedStream()
                                .forEach(listener -> listener.onSiteLinkTerminated(link, actionUserCd));
                    }
                }
            } else if ("PROPOSED".equals(link.status())) {
                // 잔존 제안 자동 취소(미러 없음 — 상태 정리만).
                if (subcon02Mapper.cancelSiteLinkBySystem(link.linkId(), actionUserCd) > 0) {
                    cancelled++;
                }
            }
        }

        log.info("관계 해지 훅 — relationId={}, 사업장 연동 독립화 {}건, 제안 자동취소 {}건", relationId, terminated, cancelled);
    }
}
