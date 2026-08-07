package com.prafta.common.cmm.schedule.vo;

/**
 * 스케줄 변경 잠금 판정 1건의 결과(prafta-com-016 공통 스케줄 변경 가드 → E3 당일분모 전환 확장).
 *
 * <p>특정 (사용자, 날짜)가 확정 연차 / 미결 시간차 신청 / 초과근무 보유로 인해 근무 스케줄을
 * 변경할 수 없을 때, 그 날짜와 사유를 운반한다. 사유는 {@link Reason} 으로 구분하며, 연차일 경우
 * 사용단위 코드(USE_UNIT_TYPE — 종일 '00' / 반차 등 / 시간차)와 미결 여부({@link #isLeavePending()})를
 * 함께 담는다(표시·필터용).
 */
public class ScheduleLockVO {

    /** 잠금 사유 구분. */
    public enum Reason {
        /** 연차 보유 — 확정(종일/반차/시간차, USE_UNIT_TYPE 무관) 또는 미결 시간차 신청(E3, leavePending). */
        LEAVE,
        /** 초과근무(등록 또는 신청) 보유. */
        OT
    }

    /** 잠긴 날짜(YYYYMMDD). */
    private final String workYmd;

    /** 잠금 사유. */
    private final Reason reason;

    /**
     * 연차 잠금일 때의 사용단위 코드(USE_UNIT_TYPE). 종일='00', 그 외 반차/시간차 코드.
     * 미결 시간차 잠금(leavePending)이면 null(REQ 에 단위 미보유 — 시간차 확정 술어라 시간민감 취급).
     * OT 잠금이면 null.
     */
    private final String leaveUseUnitType;

    /**
     * E3(당일분모 전환): 미결 연차 신청(REQ_TYPE='05', REQ_STATUS='01', 시각 보유) 잠금이면 true.
     * 확정 연차/OT 잠금이면 false.
     *
     * <p>★ 미결 잠금 대상은 <b>반차 + 시간차</b> 다(2026-08-08 반차 시간대 도입). 종전 주석의
     *   "시간차만 대상 — 반차는 잠그지 않음"은 무효다. 시각을 기록하지 않는 종일 신청만 제외된다.
     */
    private final boolean leavePending;

    public ScheduleLockVO(String workYmd, Reason reason, String leaveUseUnitType) {
        this(workYmd, reason, leaveUseUnitType, false);
    }

    public ScheduleLockVO(String workYmd, Reason reason, String leaveUseUnitType, boolean leavePending) {
        this.workYmd = workYmd;
        this.reason = reason;
        this.leaveUseUnitType = leaveUseUnitType;
        this.leavePending = leavePending;
    }

    public String getWorkYmd() {
        return workYmd;
    }

    public Reason getReason() {
        return reason;
    }

    public String getLeaveUseUnitType() {
        return leaveUseUnitType;
    }

    public boolean isLeavePending() {
        return leavePending;
    }
}
