package com.prafta.web.attd.leaveflow.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * tb_user_leave_use INSERT 운반체 (prafta-019-E).
 *
 * <p>연차 사용(차감) 1행. "신청 시 예약(CONFIRMED) → 반려 시 해제(CANCELLED)" 모델.
 */
@Getter
@Builder
public class LeaveUseVO {
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
