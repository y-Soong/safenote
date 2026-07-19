package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.SnapshotNearmissDetailResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 아차사고 수신 스냅샷 상세 응답(읽기전용 — PRAFTA-SUBCON-T7 §5-8).
 */
@Getter
@Builder
public class SnapshotNearmissDetailResponse {

    private final List<SnapshotNearmissDetailResult> rows;
}
