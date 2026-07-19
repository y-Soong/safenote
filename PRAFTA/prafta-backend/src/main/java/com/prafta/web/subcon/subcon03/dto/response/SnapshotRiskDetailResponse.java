package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.SnapshotRiskDetailResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 위험성평가 수신 스냅샷 상세 응답(읽기전용 — PRAFTA-SUBCON-T7 §5-8).
 * 성명은 평문(Q1) 그대로, 첨부는 fileMgmtCd 만 내려주고 바이트는 snapshot-file 로 별도 요청한다.
 */
@Getter
@Builder
public class SnapshotRiskDetailResponse {

    private final List<SnapshotRiskDetailResult> rows;
}
