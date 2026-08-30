package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.EvidenceWorkerSummaryResult;

import lombok.Builder;
import lombok.Value;

/** TBM 증빙 근로자별 반기 이수 집계 응답 (GET /webApi/tbm04/evidence-worker-summary). */
@Value
@Builder
public class EvidenceWorkerSummaryResponse {
    List<EvidenceWorkerSummaryResult> workerList;
}
