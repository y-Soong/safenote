package com.prafta.common.cmm.leave.promotion.vo;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

/**
 * 촉진 연차 사용({@code tb_user_leave_use}) INSERT 1행 운반체 (PRAFTA-COM-008-A-3/A-4).
 *
 * <p>{@code LeaveFlowMapper.insertLeaveUse}({@code LeaveUseVO}) 미러이되 촉진 마커
 * (PROMOTION_STAGE/DESIGNATOR_TYPE/ORIG_DESIGNATED_DATE)를 추가로 싣는다. 촉진 등록은
 * 항상 결재 없는 직접 차감(REQ_ID=NULL) · CONFIRMED · 종일(USE_UNIT_TYPE='00', LEAVE_DAYS=1.0)
 * 이므로 시각/분 컬럼은 사용하지 않는다.
 *
 * <p>멱등: DIRECT_USE_KEY(=USER|START_DATE|LEAVE_CD, DB 생성컬럼) + UNIQUE(CMPNY_CD, DIRECT_USE_KEY)
 * 가 같은 사용자·같은 일자·SYS_ANNUAL 중복 등록을 최종 차단한다(서비스가 사전 count + 충돌 흡수).
 */
@Getter
@Builder
public class PromotionLeaveUseInsertVO {

    /** 연차 사용 ID (PK) — 서비스에서 채번 */
    private String leaveId;

    private String cmpnyCd;
    private String siteCd;
    private String userCd;

    /** 연차 코드 (촉진 등록은 'SYS_ANNUAL' 고정) */
    private String leaveCd;

    /** 차감 대상 본연차 부여 ID (STATUTORY_ANNUAL ACTIVE grant) */
    private String grantId;

    /** 사용 시작일 = 종료일 = 대상일 (YYYYMMDD) */
    private String startDate;
    private String endDate;

    /** 사용 단위 [SYS025] 종일='00' */
    private String useUnitType;

    /** 사용 일수 (종일 1.0) */
    private BigDecimal leaveDays;

    /** 사용 사유 */
    private String leaveReason;

    /** 사용 상태 ('CONFIRMED') */
    private String leaveStatus;

    /** 촉진 단계 [SYS068] FIRST / SECOND */
    private String promotionStage;

    /** 지정 주체 [SYS069] VOLUNTARY / COMPANY */
    private String designatorType;

    /** 최초 촉진 지정일 (YYYYMMDD, 최초 등록 시 START_DATE 와 동일, 이동해도 보존) */
    private String origDesignatedDate;

    /** 등록자 */
    private String insertNo;
}
