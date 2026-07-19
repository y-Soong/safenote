package com.prafta.web.subcon.subcon03.dto.response;

import lombok.Builder;
import lombok.Value;

/** 승인(= 스냅샷 생성) 응답 — 생성된 스냅샷 요약. */
@Value
@Builder
public class ShareReqApproveResponse {
    Long snapshotId;
    int version;
    int rowCnt;
    int consentExcludedCnt;
    String unclosedIncludedYn;
}
