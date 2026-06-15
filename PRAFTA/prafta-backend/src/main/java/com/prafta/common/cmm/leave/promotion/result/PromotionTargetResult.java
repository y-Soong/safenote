package com.prafta.common.cmm.leave.promotion.result;

import java.math.BigDecimal;

/**
 * 연차 사용촉진 도래자 1명의 판정 결과 (PRAFTA-COM-008-A-1).
 *
 * <p>일배치({@code LeavePromotionScheduler})가 전 사용자를 스캔해 "오늘이 이 사람의 1차/2차
 * 촉진 시점인가"를 판정한 결과를 운반한다. 1차 도래자는 A-2(통지 배치), 2차 도래자는
 * 마스터에 {@code STAGE2_TARGET_DAYS} 기록 대상이 된다.
 *
 * <p>잔여({@code remainingDays})는 법정 본연차(STATUTORY_ANNUAL)+근속가산(STATUTORY_TENURE_BONUS)
 * ACTIVE grant 의 (GRANT_DAYS-USED_DAYS) 합산이다. 월차/약정은 합산하지 않는다(확정-1).
 *
 * <p>record 매핑 주의(메모리 feedback_mybatis_record_column_order): MyBatis resultType 으로 쓰지
 * 않고 서비스에서 명시 생성하므로 위치 밀림 위험 없음. 다만 매퍼 result VO 는 별도 정의한다.
 */
public record PromotionTargetResult(
        String cmpnyCd,
        String siteCd,
        String userCd,
        String baseGrantId,
        String baseAvailToDate,
        BigDecimal remainingDays,
        PromotionStage stage
) {

    /** 촉진 단계 [SYS068] FIRST/SECOND (NONE 은 비촉진이라 도래 결과에 쓰지 않음). */
    public enum PromotionStage {
        FIRST,
        SECOND
    }
}
