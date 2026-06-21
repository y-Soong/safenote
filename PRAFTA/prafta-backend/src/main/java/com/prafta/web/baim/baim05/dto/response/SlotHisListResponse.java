package com.prafta.web.baim.baim05.dto.response;

import java.util.List;

import com.prafta.web.baim.baim05.result.SlotHisResult;

import lombok.Builder;
import lombok.Value;

/**
 * PRAFTA-055-3 — 슬롯 사용 이력 목록 응답.
 */
@Value
@Builder
public class SlotHisListResponse {
    List<SlotHisResult> slotHisList;
}
