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

    /**
     * [PS-04, D-3] 마감분만 부분 포함 표식 — 'Y':커버리지 필터로 일부 제외(자체 또는 릴레이 병합) /
     * 'N':전체 포함. rowCnt 가 이미 필터 이후의 실제 포함 건수를 담고 있어 별도 includedRowCnt 필드는
     * 두지 않는다(중복 — rowCnt 로 충분).
     */
    String closedPartialYn;
}
