package com.prafta.common.cmm.schedule.vo;

/**
 * 스케줄 변경 잠금 판정 1건의 결과(prafta-com-016 공통 스케줄 변경 가드).
 *
 * <p>특정 (사용자, 날짜)가 확정 연차 또는 초과근무 보유로 인해 근무 스케줄을 변경할 수 없을 때,
 * 그 날짜와 사유를 운반한다. 사유는 {@link Reason} 으로 구분하며, 연차일 경우 사용단위 코드
 * (USE_UNIT_TYPE — 종일 '00' / 반차 등 / 시간차)를 함께 담는다(표시·필터용).
 */
public class ScheduleLockVO {

    /** 잠금 사유 구분. */
    public enum Reason {
        /** 확정 연차(종일/반차/시간차 — USE_UNIT_TYPE 무관) 보유. */
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
     * OT 잠금이면 null.
     */
    private final String leaveUseUnitType;

    public ScheduleLockVO(String workYmd, Reason reason, String leaveUseUnitType) {
        this.workYmd = workYmd;
        this.reason = reason;
        this.leaveUseUnitType = leaveUseUnitType;
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
}
