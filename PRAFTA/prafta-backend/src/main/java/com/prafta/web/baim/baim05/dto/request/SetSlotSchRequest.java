package com.prafta.web.baim.baim05.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 슬롯 기본 근무타입(DEFAULT_SCH_CD) 지정/해제 요청(단일/일괄 공용).
 *
 * <p>슬롯마다 지정 근무타입이 다를 수 있으므로 항목(Item) 안에 schCd 를 포함한다.
 * schCd 가 빈값이면 해제(NULL, 근로자 본인 선택 폴백), 값이 있으면 해당 슬롯 사업장의
 * 활성 근무타입인지 서버가 재검증한다(isValidDefaultSch — cross-site 변조 차단).
 * 점유중 슬롯도 지정 허용(메타 설정, 현재 점유자에게는 소급 미적용 — 다음 점유부터 반영).
 */
@Getter
@Setter
@NoArgsConstructor
public class SetSlotSchRequest {
    private List<Item> slots;

    /** 슬롯 단건 + 지정할 기본 근무타입(빈값=해제). */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private String siteCd;
        private String slotNo;
        private String schCd; // 빈값=기본 근무타입 해제(NULL)
    }
}
