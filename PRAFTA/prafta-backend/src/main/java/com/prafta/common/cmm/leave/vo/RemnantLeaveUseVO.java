package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * PC-05/06: 짜투리 발동·회수 use 행 INSERT 운반체.
 *
 * <p>{@code LeaveFlowMapper.insertLeaveUse}(웹 LeaveUseVO)와 동일 컬럼 구성 —
 * common 모듈이 웹 매퍼를 참조하지 않도록 매퍼별 보유 관례(LeaveHourlyResettleMapper 선례)로 미러.
 */
@Getter
@Builder
public class RemnantLeaveUseVO {

    private final String leaveId;
    private final String cmpnyCd;
    private final String siteCd;
    private final String userCd;
    /** 차감 GRANT 귀속 연차 코드(신청 코드가 아니라 부여의 LEAVE_CD — 진행파일 설계 확정 3). */
    private final String leaveCd;
    private final String reqId;
    private final String grantId;
    private final String startDate;
    private final String startTime;
    private final String endDate;
    private final String endTime;
    private final String useUnitType;
    private final BigDecimal leaveDays;
    /** 분할 INSERT 규칙: 첫 행만 신청 분을 싣는다(PC-01 REQ 합산 정합). 회수 행은 항상 null. */
    private final Integer leaveMinutes;
    private final String leaveReason;
    private final String leaveStatus;
    private final String insertNo;
}
