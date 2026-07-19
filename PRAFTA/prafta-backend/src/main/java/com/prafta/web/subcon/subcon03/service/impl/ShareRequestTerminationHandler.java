package com.prafta.web.subcon.subcon03.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.prafta.web.subcon.subcon01.result.TerminationImpactItem;
import com.prafta.web.subcon.subcon01.service.RelationTerminationHandler;
import com.prafta.web.subcon.subcon03.mapper.Subcon03Mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관계(T1) 해지 시 진행 중 데이터 공유요청 자동 종결 핸들러(PRAFTA-SUBCON-T3-07).
 *
 * <p>T1 {@link RelationTerminationHandler} 계약 구현체 — 빈 등록만으로 Subcon01ServiceImpl 의
 * ObjectProvider 주입에 수집된다(T1·T2 코드 무수정, T2 SiteLinkTerminationHandler 패턴 승계).
 *
 * <ul>
 *   <li>summarize: 관계 산하 REQUESTED 공유요청 건수 요약(해지 확인 팝업 표시 — 부작용 금지).</li>
 *   <li>onTerminated: REQUESTED → CANCELLED 만 수행(고아 요청 방지). 동일 트랜잭션 —
 *       예외 시 관계 해지 전체 롤백(T1 훅 계약).</li>
 *   <li><b>불변식(결정 3)</b>: 이미 생성된 스냅샷·상세행·번들은 <b>무접촉</b>이다. 수신사는 연동이
 *       종료돼도 보유 자료를 읽기전용으로 계속 열람한다. 본 클래스에 스냅샷 DELETE 코드가
 *       단 한 줄도 들어가서는 안 된다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShareRequestTerminationHandler implements RelationTerminationHandler {

    private final Subcon03Mapper subcon03Mapper;

    @Override
    public List<TerminationImpactItem> summarize(long relationId, String cmpnyCdA, String cmpnyCdB) {
        int requestedCnt = subcon03Mapper.selectRequestedCntByRelation(relationId);

        List<TerminationImpactItem> impacts = new ArrayList<>();
        if (requestedCnt > 0) {
            impacts.add(new TerminationImpactItem(
                    "SHARE_REQ", "진행 중 데이터 공유요청 자동취소 예정", requestedCnt));
        }
        return impacts;
    }

    @Override
    public void onTerminated(long relationId, String cmpnyCdA, String cmpnyCdB, String actionUserCd) {
        // REQUESTED 요청만 정리한다. 스냅샷/상세행/번들은 건드리지 않는다(수신 자료 존속 — 결정 3).
        int cancelled = subcon03Mapper.cancelShareReqByRelation(relationId, actionUserCd);

        if (cancelled > 0) {
            log.info("관계 해지 훅 — relationId={}, 데이터 공유요청 자동취소 {}건(스냅샷은 존속)", relationId, cancelled);
        }
    }
}
