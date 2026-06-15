package com.prafta.web.baim.baim05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 슬롯 단건 식별 요청 항목(단일/일괄 공용).
 *
 * <p>currUserCd 는 클라가 보내더라도 서버에서 신뢰하지 않는다(IDOR 방지).
 * 점유자 식별은 서버가 슬롯 PK 로 CURR_USER_CD 를 재조회해서 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class SlotItemRequest {
    private String siteCd;
    private String slotNo;
    private String currUserCd; // 참고용(서버 미신뢰)
}
