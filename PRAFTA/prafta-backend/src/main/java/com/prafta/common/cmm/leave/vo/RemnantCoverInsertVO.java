package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * PC-05(D6): TB_LEAVE_REMNANT_COVER INSERT 운반체 (발동 시 회사 부담분 기록).
 */
@Getter
@Builder
public class RemnantCoverInsertVO {

    private final String coverId;
    private final String cmpnyCd;
    private final String siteCd;
    private final String userCd;
    private final String reqId;
    private final String workYmd;
    private final String useUnitType;
    private final BigDecimal chargeDays;
    private final BigDecimal remnantDays;
    private final BigDecimal coverDays;
    private final Integer coverMinutes;
    private final Integer convMinutes;
    private final String insertNo;
}
