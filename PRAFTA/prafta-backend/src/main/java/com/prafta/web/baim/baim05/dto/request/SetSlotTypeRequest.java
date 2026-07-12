package com.prafta.web.baim.baim05.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 슬롯 구분(SLOT_TYPE, SYS014 발급채널) 변경 요청(단일/일괄 공용).
 *
 * <p>슬롯마다 구분 값이 다르므로 항목(Item) 안에 slotType 을 포함한다
 * (SetSlotFixedRequest 가 단일 fixedYn 인 것과 다름).
 * 비점유 슬롯만 변경 허용이며, 점유 가드는 서버가 신뢰 원천이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetSlotTypeRequest {
    private List<Item> slots;

    /** 슬롯 단건 + 변경할 구분 값. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private String siteCd;
        private String slotNo;
        private String slotType; // SYS014: '01'=직접가입, '02'=QR발급
    }
}
