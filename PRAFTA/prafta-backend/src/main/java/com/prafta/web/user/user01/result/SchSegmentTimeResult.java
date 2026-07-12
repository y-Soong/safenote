package com.prafta.web.user.user01.result;

/**
 * 기본 근무타입의 effective(적용일 기준) 근무 구간 시각 — PRAFTA-WEB_001-1.
 *
 * <p>불가케이스 ⑤(시간차 연차 미커버) 판정에 사용한다. 1구간(FST)은 필수, 2구간(SEC)은 선택.
 * 시각은 "HHmm" 4자리 문자열(예: "0900"). 2구간 미사용 시 SEC_* 는 NULL/빈문자.
 */
public record SchSegmentTimeResult(
    String fstSchStrTime
    , String fstSchEndTime
    , String secSchStrTime
    , String secSchEndTime
) {}
