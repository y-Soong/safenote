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
    /** 증빙 파일 ID(연차 신청 증빙 필수화 2026-08-29). 분할 차감 시 첫 charge 행에만 저장(leaveMinutes 관례 동일). */
    private String evidenceFileId;
    private String leaveStatus;
    private String insertNo;
    /** BW-04: 휴게시간 무시 요청 'Y'/'N'. 분할 차감 시 모든 행 동일 값(판정 속성). REQ_DTIME 은 매퍼가 NOW() 로 채운다. */
    private String brkWaiveYn;
    /**
     * v2(BW2-04): 넘긴 휴게 분량. 반차 = 적용 W_eff / 시간차 = 편입 휴게분 / 기록 전용 = 0.
     * 매퍼가 {@code BRK_WAIVE_YN='Y'} 일 때만 {@code IFNULL(값, 0)} 로 저장하고 'N' 이면 NULL. 분할 차감 시 전 행 동일 값.
     */
    private Integer brkWaiveMin;
}
