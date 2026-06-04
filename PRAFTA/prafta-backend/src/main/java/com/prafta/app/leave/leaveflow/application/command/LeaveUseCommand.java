package com.prafta.app.leave.leaveflow.application.command;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-018-B: tb_user_leave_use INSERT 운반체.
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.vo.LeaveUseVO} 미러.
 * 연차 사용(차감) 1행. "신청 시 예약(CONFIRMED)" 모델.
 *   INSERT 는 이름 기반 {@code #{...}} 매핑이라 필드 순서는 위치매핑 함정 비해당.
 */
@Getter
@Builder
public class LeaveUseCommand {
    private String leaveId;
    private String cmpnyCd;
    private String siteCd;
    private String userCd;
    private String leaveCd;
    private String reqId;
    private String grantId;
    private String startDate;
    private String startTime;
    private String endDate;
    private String endTime;
    private String useUnitType;
    private BigDecimal leaveDays;
    private Integer leaveMinutes;
    private String leaveReason;
    private String leaveStatus;
    private String insertNo;
}
