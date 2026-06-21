package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 연차 타입(관리자 부여)의 "사용 가능 기간" 설정 조회 결과 (prafta-045).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1(사용 가능 기간 속성).
 *
 * <p>관리자 수동 부여(LeaveDashboardServiceImpl.manualGrant) 시 부여건의 AVAIL_TO_DATE를
 * 회사 공통 유효개월(AXIS6)이 아니라 해당 타입의 사용가능기간 설정으로 산출하기 위해 조회한다.
 * 수동 부여 타입은 항상 관리자 부여 타입(LEAVE_TYPE='02' AND GRANT_TYPE='02')이므로
 * 관리자 전용 컬럼(ADMIN_AVAIL_*)을 읽는다. 사용자신청 타입의 AVAIL_*(MMDD)는 무관하다.
 *
 * <p>조회는 CMPNY_CD 스코프로 격리한다(타사 타입 avail-term 조회 차단).
 */
@Getter
@Setter
public class LeaveTypeAvailTermVO {

    /** 사용가능기간 타입 [SYS026] 01:설정안함(무기한) / 02:해당연도내 / 03:기간설정. null=미설정. */
    private String adminAvailTermType;

    /** 기간설정('03') 시 사용기간 FROM (YYYYMMDD 8자, prafta-044-FU2). prafta-com-016-B 이후 미사용. 그 외 null. */
    private String adminAvailFromDt;

    /** 기간설정('03') 시 사용기간 TO (YYYYMMDD 8자, prafta-044-FU2). prafta-com-016-B 이후 미사용. 그 외 null. */
    private String adminAvailToDt;

    /**
     * prafta-com-016-B(3-2): 기간설정('03') 시 "부여일로부터 N개월"(1~99 정수). 그 외 null.
     * 만료(AVAIL_TO_DATE) = 부여일 + N개월의 해당일(존재 안 하는 날은 말일 보정).
     */
    private Integer adminAvailMonths;
}
