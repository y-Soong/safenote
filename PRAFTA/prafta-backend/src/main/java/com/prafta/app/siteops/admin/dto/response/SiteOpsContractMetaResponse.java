package com.prafta.app.siteops.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 현장 처리 계약서 메타 응답 (GET /appApi/admin/site-ops/contract/meta — pager 초기화).
 *
 * <p>대상 계약서는 core(DailyContractService)가 근로자 본인 서명 경로와 동일한 리졸버
 * (승인 시점 pin 우선)로 결정한다 — "관리자 폰에서 읽는 문서 = 서명 대상 문서" 보장.
 */
@Getter
@Builder
public class SiteOpsContractMetaResponse {

    /** 서명 대상 계약서 버전. */
    private final int contractVer;

    /** 계약서명(표시용). */
    private final String contractNm;

    /** 문서 형식: 'PDF' | 'IMG'. */
    private final String formatType;

    /** 페이지 수(pager 구성용, 1 이상). */
    private final int pageCount;
}
