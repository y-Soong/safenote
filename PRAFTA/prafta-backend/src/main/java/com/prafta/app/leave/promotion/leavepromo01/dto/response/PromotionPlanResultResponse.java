package com.prafta.app.leave.promotion.leavepromo01.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-008-A-3: 앱 1차 계획서 등록(plan) 결과 응답.
 *
 * <p>다건 날짜 중 등록 성공/스킵(중복)/실패(잔여부족·마감·비근무일) 집계와, 등록 후 갱신된 잔여를 싣는다.
 * 일부 지정 허용(전체 의무 아님)이므로 일부 실패가 있어도 성공분은 확정한다.
 */
@Getter
@Builder
public class PromotionPlanResultResponse {

    /** 신규 등록된 날짜 목록 (YYYYMMDD). */
    private List<String> registeredDates;

    /** 이미 등록되어 스킵된 날짜 목록 (YYYYMMDD). */
    private List<String> skippedDates;

    /** 등록 실패한 날짜 목록 (YYYYMMDD) — 잔여부족/마감/비근무일/출근기록 존재(§9.4) 등. */
    private List<String> failedDates;

    /** 등록 후 갱신된 미지정 잔여 연차. */
    private BigDecimal remainingDays;
}
