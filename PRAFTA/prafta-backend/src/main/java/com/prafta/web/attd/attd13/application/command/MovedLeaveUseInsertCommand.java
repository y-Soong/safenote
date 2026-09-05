package com.prafta.web.attd.attd13.application.command;

import java.math.BigDecimal;

/**
 * T1(이동 재정의): 이동 재차감 use 행 INSERT 운반체 — 속성 승계(§2-4) 포함.
 *
 * <p>{@code LeaveFlowMapper.insertLeaveUse} 는 승계 컬럼(PROMOTION_STAGE / DESIGNATOR_TYPE /
 * ORIG_DESIGNATED_DATE)을 싣지 않으므로(§0-1-6, writer 불변 원칙) Attd13 전용 INSERT 를 신설한다.
 *
 * <ul>
 *   <li>{@code leaveDays} = 재산출 charge 분할값 / {@code leaveMinutes} = 첫 행만 원 총량(불변식 1)</li>
 *   <li>{@code origDesignatedDate} = IFNULL(원 대표행 기존값, 원 START_DATE) — 최초 지정일 보존</li>
 *   <li>{@code promotionStage}/{@code designatorType} = 원 대표행 값(노무수령거부 판정 연속성 — attd §8.3)</li>
 *   <li>{@code evidenceFileId} = 원 대표행 값(F6/qa D-8 — 증빙 링크 유실 방지, LEAVE_REASON 과 동일하게 전 행 승계)</li>
 * </ul>
 */
public record MovedLeaveUseInsertCommand(
      String leaveId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String leaveCd
    , String reqId
    , String grantId
    , String startDate
    , String startTime
    , String endDate
    , String endTime
    , String useUnitType
    , BigDecimal leaveDays
    , Integer leaveMinutes
    , String leaveReason
    , String evidenceFileId
    , String promotionStage
    , String designatorType
    , String origDesignatedDate
    , String insertNo
    /** BW-04(Q-1 승계): 원 행 BRK_WAIVE_YN 그대로 복사('Y'/'N', null=N). */
    , String brkWaiveYn
    /** BW-04(Q-1 승계): 원 행 BRK_WAIVE_REQ_DTIME('yyyy-MM-dd HH:mm:ss', null 가능) 그대로 복사 — 요청 시각 보존. */
    , String brkWaiveReqDtime
    /**
     * v2(BW2-06 승계): 넘긴 휴게 분량. 반차 = 대상일 실효 cap 으로 클램프한 W_eff(서비스 산출) / 시간차 = 원 값 복사(R4 재검증 통과 시)
     * / 시각 없는 구 반차·NULL 원 행 = NULL 승계 허용(매퍼는 IFNULL 없이 그대로 저장, 'N' 이면 NULL).
     */
    , Integer brkWaiveMin
) {
}
