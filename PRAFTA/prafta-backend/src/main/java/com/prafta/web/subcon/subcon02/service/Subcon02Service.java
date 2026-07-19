package com.prafta.web.subcon.subcon02.service;

import com.prafta.web.subcon.subcon02.application.param.ChkptLinkParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkListParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkProcessParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkProposeParam;
import com.prafta.web.subcon.subcon02.dto.response.LinkProposeCandidatesResponse;
import com.prafta.web.subcon.subcon02.dto.response.SiteLinkListResponse;
import com.prafta.web.subcon.subcon02.dto.response.SiteLinkProposeResponse;

public interface Subcon02Service {

    /** 사업장 연동 링크 목록(자사 당사자 전 상태 — 프론트 제공/받은 2분류, 목록=이력). */
    SiteLinkListResponse selectSiteLinkList(SiteLinkListParam param);

    /** 연동 제안 후보(관계 ACCEPTED 상대 회사 + 내 활성 사업장 — 미러 포함). */
    LinkProposeCandidatesResponse selectProposeCandidates(SiteLinkListParam param);

    /** 연동 제안 생성(§5-3 가드 5종: 소유/관계/자기회사/루프/중복). */
    SiteLinkProposeResponse proposeSiteLink(SiteLinkProposeParam param);

    /** 수락 = 미러 생성 트랜잭션(PROPOSED→ACTIVE + TB_SITE/노드/권한/근무타입 복제 — §5-4). */
    void acceptSiteLink(SiteLinkProcessParam param);

    /** 거부(PROPOSED→REJECTED, DST 소속만, 사유 필수 ≤500자). */
    void rejectSiteLink(SiteLinkProcessParam param);

    /** 취소(PROPOSED→CANCELLED, SRC 소속만). */
    void cancelSiteLink(SiteLinkProcessParam param);

    /** 해지 = 독립화(ACTIVE→TERMINATED + 미러 LINK_SRC NULL 화, 양측 가능 — §5-6). */
    void terminateSiteLink(SiteLinkProcessParam param);

    /**
     * PRAFTA-SUBCON-T6-02: 순회점검 구성 연동 실행(SRC 소속만).
     * 활성 점검대상 + 사업장 문항을 수신 미러 사업장에 복제하고 점검연동 ACTIVE 하위 체인으로 재귀 확장한다.
     */
    void enableChkptLink(ChkptLinkParam param);

    /**
     * PRAFTA-SUBCON-T6-02: 순회점검 구성 연동 해제(양측 가능) = 점검 미러 독립화.
     * 잠금 해제 + 결과 통합 중단. 기존 실적은 전량 보존한다.
     */
    void disableChkptLink(ChkptLinkParam param);
}
