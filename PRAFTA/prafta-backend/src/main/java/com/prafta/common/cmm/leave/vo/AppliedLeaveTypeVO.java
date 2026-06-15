package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 신청형 휴가(사용자 신청 LEAVE_TYPE='01') 1종 잔여 현황 (연차 대시보드 상세 — 신청형 휴가 섹션).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1
 *
 * <p>법정연차(STATUTORY_*) / 관리자부여(MANUAL_, LEAVE_TYPE='02') 그룹과 절대 합산하지 않고
 * 타입별 별도 항목으로 노출한다. 잔여 = 한도(MAX_APLY_DAYS) − 당해 회계연도 사용(CONFIRMED 합계).
 *
 * <p>한도(MAX_APLY_DAYS)는 NULL 허용이며, NULL이면 한도 0 = 잔여 0(fail-closed)으로 본다(서비스 산출).
 * 사용/잔여는 정수 일수(MAX_APLY_DAYS tinyint, LEAVE_DAYS 합계도 신청형은 일단위)로 다룬다.
 */
@Getter
@Setter
public class AppliedLeaveTypeVO {

    /** 연차 코드 (tb_leave_type_mgmt.LEAVE_CD) */
    private String leaveCd;

    /** 연차명 */
    private String leaveNm;

    /** 한도 = MAX_APLY_DAYS (tinyint unsigned, NULL 허용 → 서비스에서 0으로 fail-closed) */
    private Integer maxAplyDays;

    /** 당해 회계연도 사용 일수 합계 (TB_USER_LEAVE_USE CONFIRMED, START_DATE 회계연도 경계 내) */
    private Integer usedDays;

    /** 잔여 = 한도 − 사용 (서비스 산출, 음수면 0 클램프 — 표시 안정성) */
    private Integer remainDays;
}
