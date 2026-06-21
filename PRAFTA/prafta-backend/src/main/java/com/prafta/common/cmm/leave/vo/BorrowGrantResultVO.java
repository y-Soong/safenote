package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 가불 GRANT 생성 결과 (prafta-com-011 가불 코어).
 *
 * <p>출처: {@code .claude/requests/common/prafta-com-011-decisions.md} §6.
 *
 * <p>가불 신청 1건은 월차의 경우 여러 미래 월 슬롯(입사+m개월)에 1일씩 분할 생성될 수 있으므로,
 * 생성된 가불 GRANT 들을 슬롯 단위로 운반한다. 호출부(com-011-2 신청 흐름)는 각 슬롯의 grantId 로
 * leave_use 를 분할 INSERT 하여 차감한다.
 *
 * <ul>
 *   <li>{@code slots} : 실제 생성(또는 이미 정기 부여되어 skip)된 슬롯 목록.</li>
 *   <li>{@code createdDays} : 이번 호출로 신규 생성된 가불 일수 합(멱등 skip 분 제외).</li>
 *   <li>{@code skippedDays} : 멱등키 충돌(이미 정기 부여됨)로 생성하지 않은 일수 합.</li>
 * </ul>
 */
@Getter
@Builder
public class BorrowGrantResultVO {

    private List<BorrowGrantSlotVO> slots;
    private BigDecimal createdDays;
    private BigDecimal skippedDays;

    /** 가불 GRANT 1슬롯(부여 ID + 일수 + 슬롯 라벨 + 신규생성 여부). */
    @Getter
    @Builder
    public static class BorrowGrantSlotVO {
        /** 채번된 가불 GRANT_ID. 멱등 skip(이미 정기 부여) 슬롯이면 null. */
        private String grantId;
        /** 이 슬롯에 충당한 일수. */
        private BigDecimal days;
        /** 슬롯 식별 라벨(월차=YYYYMM, 본연차=차기 발생연도 YYYY) — 멱등키 라벨과 동일. */
        private String periodLabel;
        /** 슬롯 종류(STATUTORY_MONTHLY / STATUTORY_ANNUAL / STATUTORY_TENURE_BONUS). */
        private String grantType;
        /** 실제 신규 INSERT 발생 여부(false면 멱등키 충돌로 skip — 이미 정기 부여됨). */
        private boolean created;
    }
}
