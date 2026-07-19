package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.SnapshotResult;

import lombok.Builder;
import lombok.Value;

/** 수신 보유 스냅샷 목록 응답(자사 소유분만 — OWNER_CMPNY_CD 스코프). */
@Value
@Builder
public class SnapshotListResponse {
    List<SnapshotResult> snapshots;
}
