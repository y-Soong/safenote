package com.prafta.web.acct.acct01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수평선 [확인] — 연계 도메인 확정 결과를 tb_acct_link 에 스냅샷 저장하는 요청.
 * 도메인당 다건. items 각 항목은 원본키(JSON) + 확정 시점 값(JSON).
 */
@Getter
@Setter
@NoArgsConstructor
public class LinkConfirmRequest {
    private String siteCd;
    private String acctId;
    private String linkDomainCd; // SYS067 ATTD/CHKPT/RISK/TBM/NEAR_MISS
    private List<LinkItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LinkItem {
        private String linkKeyJson;  // 연결 원본키 묶음(JSON 문자열)
        private String snapshotJson; // 확정 시점 조회값(JSON 문자열)
    }
}
