package com.prafta.web.baim.baim05.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 슬롯 점유 고정여부 토글 요청(단일/일괄 공용).
 * fixedYn='Y' 점유 유지, 'N' 점유 해지.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetSlotFixedRequest {
    private List<SlotItemRequest> slots;
    private String fixedYn;
}
