package com.prafta.web.subcon.subcon03.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon03.result.SnapshotDetailResult;

import lombok.Builder;
import lombok.Value;

/**
 * 수신 스냅샷 상세 응답(읽기전용 페이지).
 *
 * <p>소유 검증은 조회 SQL 안에서 강제한다 — 타사 스냅샷ID 로 호출하면 빈 목록이 반환된다(존재 비노출).
 */
@Value
@Builder
public class SnapshotDetailResponse {
    List<SnapshotDetailResult> rows;
    int page;
    int pageSize;
}
