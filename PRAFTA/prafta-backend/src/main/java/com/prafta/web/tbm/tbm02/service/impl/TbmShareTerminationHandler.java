package com.prafta.web.tbm.tbm02.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.tbmshare.service.TbmSessionShareService;
import com.prafta.web.subcon.subcon01.result.TerminationImpactItem;
import com.prafta.web.subcon.subcon01.service.RelationTerminationHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관계(T1) 해지 시 TBM 연동 회사 지정 자동 회수 핸들러(PRAFTA-SUBCON-T5-03).
 *
 * <p>T1 {@link RelationTerminationHandler} 계약 구현체 — 빈 등록만으로 Subcon01ServiceImpl 의
 * ObjectProvider 주입에 수집된다(T1 코드 무수정. T2/T3 선례 동형).
 *
 * <ul>
 *   <li>summarize: 해제 예정 지정 건수 요약(하위 캐스케이드 포함, 부작용 금지).</li>
 *   <li>onTerminated: 두 회사 사이의 유효 지정 전부 + 그 하위 재지정 체인을 DEL_YN='Y' 로 해제
 *       (RELEASE_REASON_CD='RELATION_TERMINATED'). 동일 트랜잭션 — 예외 시 관계 해지 전체 롤백.</li>
 *   <li><b>불변식</b>: TB_TBM_ATTENDANCE 는 단 한 행도 건드리지 않는다. 이미 입실한 참석자는
 *       그대로 유지되고 신규 입실만 차단된다(요청서 §3.1).</li>
 *   <li>IN_PROGRESS/COMPLETED 세션의 지정도 해제한다(입실이 끝난 뒤이므로 무해).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TbmShareTerminationHandler implements RelationTerminationHandler {

    private final TbmSessionShareService tbmSessionShareService;

    @Override
    public List<TerminationImpactItem> summarize(long relationId, String cmpnyCdA, String cmpnyCdB) {
        int count = tbmSessionShareService.countSharesByRelation(cmpnyCdA, cmpnyCdB);

        List<TerminationImpactItem> impacts = new ArrayList<>();
        if (count > 0) {
            impacts.add(new TerminationImpactItem(
                    "TBM_SHARE", "TBM 교육 연동회사 지정 해제 예정", count));
        }
        return impacts;
    }

    @Override
    public void onTerminated(long relationId, String cmpnyCdA, String cmpnyCdB, String actionUserCd) {
        int released = tbmSessionShareService.releaseByRelation(cmpnyCdA, cmpnyCdB, actionUserCd);

        log.info("관계 해지 훅 — relationId={}, TBM 연동회사 지정 해제 {}건(참석행 무접촉)", relationId, released);
    }
}
