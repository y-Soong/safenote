package com.prafta.common.cmm.leave.promotion.vo;

/**
 * 촉진 연차 1일 등록 결과 (PRAFTA-COM-008-A-3/A-4).
 *
 * <p>{@code LeavePromotionRegistrationService.register} 의 단건 결과. 1차/2차 등록 모두 공유한다.
 */
public enum PromotionRegisterResult {

    /** 정상 등록(leave_use INSERT + grant 차감 완료). */
    REGISTERED,

    /** 이미 같은 사용자·일자·SYS_ANNUAL 로 등록됨(멱등 스킵). */
    SKIPPED_DUP,

    /** 차감 가능한 본연차(STATUTORY_ANNUAL ACTIVE) 잔여 부족. */
    INSUFFICIENT,

    /** 교대팀 소속일인데 해당 일자에 근무 스케줄(work_plan)이 없어 등록 불가. */
    NOT_SCHEDULED,

    /** 대상 월이 근태 마감됨(쓰기 차단). */
    CLOSED,

    /** 해당 일자에 출근 근태가 존재하여 등록 거부(§9.4 상호배제, B-M1 — 퇴근 트랩 원천 차단). */
    ATTENDANCE_EXISTS
}
