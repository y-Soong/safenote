package com.prafta.common.cmm.leave.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 직원별 연차 상세(attd09)의 연도별 사용 이력 1행(매퍼 결과, 일자 전개 완료본).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.1
 *
 * <p>기간형(START_DATE~END_DATE) 종일 연차는 매퍼의 date_seq CTE가 일자 단위로 전개하므로
 * 같은 REQ_ID가 일수만큼 반복 등장한다(dateYmd만 다름) — Attd_16 연차 사용 현황 캘린더와 동일 전개 방식.
 *
 * <p>{@code status}는 dateYmd를 오늘과 비교해 매퍼(SQL)에서 산출한다: 오늘 이하면 'USED'(사용),
 * 오늘 초과면 'SCHEDULED'(사용예정) — 둘 다 LEAVE_STATUS='CONFIRMED' 범위(취소건은 조회 대상 아님).
 */
@Getter
@Setter
public class LeaveUsageHistoryRowVO {

    /** 사용 일자 (YYYYMMDD) */
    private String dateYmd;

    /** 휴가 종류 코드 */
    private String leaveCd;

    /** 휴가 종류명 */
    private String leaveNm;

    /** 사용 단위 [SYS041] (종일/오전반차/오후반차/시간차 등) */
    private String useUnitType;

    /** 시간차 사용 시작 시각 (HHmm, 종일/반차는 NULL) */
    private String startTime;

    /** 시간차 사용 종료 시각 (HHmm, 종일/반차는 NULL) */
    private String endTime;

    /** 사용 일수 (분할차감 REQ 합산 완료값) */
    private BigDecimal leaveDays;

    /** 시간차 사용 분 (종일/반차는 NULL) */
    private Integer leaveMinutes;

    /** 상태 ('USED'=사용 / 'SCHEDULED'=사용예정) — dateYmd vs 오늘 기준 매퍼 산출 */
    private String status;
}
