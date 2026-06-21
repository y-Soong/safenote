package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 기존 가불 GRANT 의 잔여 capacity 조회 결과 (prafta-com-011 QA D1/D2 통일 모델).
 *
 * <p>출처: {@code .claude/requests/common/prafta-com-011-decisions.md} §6-2(누적 가불).
 *
 * <p>같은 슬롯/회차의 멱등키로 이미 생성된 가불 GRANT 가 있으면, 신규 슬롯을 다시 INSERT 하지 않고
 * 이 GRANT 의 잔여(GRANT_DAYS − USED_DAYS)에 leave_use 를 추가하여 누적 가불한다. live(STATUS='ACTIVE'
 * AND DEL_YN='N')이며 가불 마커({@code GRANT_REASON LIKE '[가불]%'})인 단건을 멱등키로 조회한다.
 */
@Getter
@Setter
public class BorrowGrantCapacityVO {
    /** 가불 GRANT ID. */
    private String grantId;
    /** 전량 부여 일수(월차 1.0 / 본연차 차기 전량). */
    private BigDecimal grantDays;
    /** 이미 차감된 가불 사용 일수. */
    private BigDecimal usedDays;
}
