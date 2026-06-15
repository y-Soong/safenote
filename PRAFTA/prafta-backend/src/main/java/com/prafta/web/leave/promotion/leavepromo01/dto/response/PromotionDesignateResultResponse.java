package com.prafta.web.leave.promotion.leavepromo01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-com-008-A-4: 2차 회사직권 지정 결과 응답.
 *
 * <p>다건 날짜 중 등록 성공/스킵(중복)/실패(잔여부족·마감·비근무일) 집계. 일부 성공도 확정한다.
 */
@Getter
@Builder
public class PromotionDesignateResultResponse {

    /** 신규 직권지정된 날짜 목록 (YYYYMMDD). */
    private List<String> designatedDates;

    /** 이미 등록되어 스킵된 날짜 목록 (YYYYMMDD). */
    private List<String> skippedDates;

    /** 지정 실패한 날짜 목록 (YYYYMMDD). */
    private List<String> failedDates;
}
