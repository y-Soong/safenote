package com.prafta.web.baim.baim05.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 슬롯 소속부서(NODE_CD) 지정/해제 요청(단일/일괄 공용).
 *
 * <p>슬롯마다 지정 부서가 다르므로 항목(Item) 안에 nodeCd 를 포함한다.
 * nodeCd 가 빈값이면 부서 해제(NULL), 값이 있으면 해당 슬롯 사업장의 유효 노드인지
 * 서버가 재검증한다(cross-site 노드 변조 차단). 점유중 슬롯도 부서 지정 허용.
 */
@Getter
@Setter
@NoArgsConstructor
public class SetSlotNodeRequest {
    private List<Item> slots;

    /** 슬롯 단건 + 지정할 소속부서(빈값=해제). */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        private String siteCd;
        private String slotNo;
        private String nodeCd; // 빈값=부서 해제(NULL)
    }
}
