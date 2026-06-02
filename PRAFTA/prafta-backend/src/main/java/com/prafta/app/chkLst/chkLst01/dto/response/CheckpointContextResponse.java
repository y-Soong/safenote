package com.prafta.app.chkLst.chkLst01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-011: 체크포인트 컨텍스트 응답 — checklist-infos 의 checkpoint 필드.
 * <p>TB_CHKPT_TYPE_MGMT + TB_SITE 조인 결과에서 산출.
 */
@Getter
@Builder
public class CheckpointContextResponse {

    /** 체크포인트명 (TB_CHKPT_TYPE_MGMT.CHKPT_NM) */
    private final String chkptNm;

    /** 사업장명 (TB_SITE.SITE_NM) */
    private final String siteNm;

    /** 체크리스트 타입 (TB_CHKPT_TYPE_MGMT.CHKLST_TYPE) */
    private final String chklstType;

    /** 체크포인트 비고 (TB_CHKPT_TYPE_MGMT.CHKPT_DESC) */
    private final String chkptDesc;

    /** 전체 점검 항목 수 */
    private final int totalCount;
}
