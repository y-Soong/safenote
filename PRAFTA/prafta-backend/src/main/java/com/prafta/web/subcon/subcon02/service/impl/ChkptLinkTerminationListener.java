package com.prafta.web.subcon.subcon02.service.impl;

import org.springframework.stereotype.Component;

import com.prafta.web.subcon.subcon02.mapper.ChkptLinkMapper;
import com.prafta.web.subcon.subcon02.result.SiteLinkRaw;
import com.prafta.web.subcon.subcon02.service.ChkptLinkMirrorService;
import com.prafta.web.subcon.subcon02.service.SiteLinkTerminationListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사업장 링크 해지 → 순회점검 미러 독립화 리스너(PRAFTA-SUBCON-T6-08).
 *
 * <p>빈 등록만으로 해지 경로 2곳(개별 해지 / 관계 해지 캐스케이드)에 연결된다.
 *
 * <ul>
 *   <li>미러 점검대상·문항 LINK_SRC_* NULL 화 → 잠금 해제(수신사 자체 구성으로 전환).</li>
 *   <li>tb_site_link.CHKPT_LINK_STATUS='NONE' → 결과 통합/전파 중단.</li>
 *   <li>이력(HIST)·기존 실적(응답·조치·사진)은 전량 보존(무접촉).</li>
 *   <li>하위 체인 무접촉 — 수신사가 새 루트가 된다(T2 D8 승계).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChkptLinkTerminationListener implements SiteLinkTerminationListener {

    private final ChkptLinkMapper chkptLinkMapper;
    private final ChkptLinkMirrorService chkptLinkMirrorService;

    @Override
    public void onSiteLinkTerminated(SiteLinkRaw link, String actionUserCd) {
        if (link == null || link.dstSiteCd() == null) {
            return;
        }

        int statusCleared = chkptLinkMapper.clearChkptLinkStatusBySystem(link.linkId(), actionUserCd);
        chkptLinkMirrorService.independizeChkptMirror(link, actionUserCd);

        log.info("사업장 링크 해지 훅 — 점검 연동 해제 link={}, 상태해제={}행", link.linkId(), statusCleared);
    }
}
