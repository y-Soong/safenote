package com.prafta.web.acct.acct01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ①탭 — 확정된 연계 스냅샷 조회 요청.
 * linkDomainCd 미지정 시 전 도메인 스냅샷을 조회한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkSnapshotRequest {
    private String siteCd;
    private String acctId;
    private String linkDomainCd; // 선택 필터
}
