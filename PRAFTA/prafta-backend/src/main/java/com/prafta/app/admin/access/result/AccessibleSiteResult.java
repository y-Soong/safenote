package com.prafta.app.admin.access.result;

import lombok.Getter;
import lombok.Setter;

/**
 * 001-P1-B1: 접근 가능 사업장(현장 전환 셀렉터용) 조회 결과.
 *
 * <p>[권한매트릭스 §3.1] TB_USER_SITE_AUTH(USE_YN='Y') ⨝ TB_SITE(USE_YN='Y') 결과 행.
 * <p>D6 확정: 사업장명 소스는 TB_SITE.SITE_NM. PII 아님(사업장 마스터 정보).
 */
@Getter
@Setter
public class AccessibleSiteResult {
    private String siteCd;
    private String siteNo;
    private String siteNm;
}
