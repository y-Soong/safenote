package com.prafta.web.baim.baim05.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 슬롯 비우기 요청(단일/일괄 공용). slots 에 비울 슬롯 리스트를 담는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClearDailyUserSlotsRequest {
    private List<SlotItemRequest> slots;
}
