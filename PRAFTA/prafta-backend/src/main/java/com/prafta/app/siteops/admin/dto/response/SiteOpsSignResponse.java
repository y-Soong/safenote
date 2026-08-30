package com.prafta.app.siteops.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 현장 처리 계약서 서명 저장 응답 (POST /appApi/admin/site-ops/contract/sign).
 *
 * <p>서명 주체는 일용직 근로자 본인이다(관리자 폰을 근로자에게 전달해 직접 서명 — 대리 입력 아님).
 * 합성/저장/멱등은 core(DailyContractService.signContract)가 근로자 앱 서명과 동일 경로로 수행한다.
 */
@Getter
@Builder
public class SiteOpsSignResponse {

    /** 처리 결과: SIGNED. */
    private final String result;

    /** 생성된 서명 ID. */
    private final String signId;
}
