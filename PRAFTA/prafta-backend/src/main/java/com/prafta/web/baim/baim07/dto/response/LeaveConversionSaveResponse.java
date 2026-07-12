package com.prafta.web.baim.baim07.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 시간차 1일 환산시간 저장 응답 (LC-02). baim07 POST /conversion.
 *
 * <p>저장 시 안내 문구(적용일 이후 신청분부터 반영 — F4 소급 재계산 없음)를 함께 반환한다.
 */
@Value
@Builder
public class LeaveConversionSaveResponse {

    /** 저장된 적용 시작일(YYYYMMDD). */
    String applyFromDate;

    /** 저장된 1일 환산시간(분). */
    int dailyConvMinutes;

    /** 화면 안내 문구(plan LC-02-②). */
    String message;
}
