package com.prafta.common.cmm.leave.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * TB_LEAVE_POLICY 단건/스냅샷 운반체.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2 (7개 axis)
 *
 * <p>본 VO는 PRAFTA-018 정책 관리(baim07) + STATUS 관련 서비스에서 공용한다.
 * 7개 axis 컬럼 + 활성 메타데이터(USE_YN, APPLY_FROM_DATE, INSERT_NO/UPDATE_NO 등)를 운반한다.
 *
 * <p>USAGE_POLICY (1:1)는 별도 VO로 분리하지 않고 본 VO에 합쳐서 운반한다
 * (단일 트랜잭션 내에서 같이 INSERT/UPDATE 되므로).
 */
@Getter
@Setter
public class LeavePolicyVO {

    /** 정책 일련번호 (PK, AUTO_INCREMENT bigint) */
    private Long policySeq;

    /** 회사 코드 */
    private String cmpnyCd;

    /** 프리셋 분류: HIRE_DATE / FISCAL_PRORATE / FISCAL_MONTHLY / HIRE_DATE_PREGRANT / CUSTOM */
    private String policyPreset;

    // ===== 7개 axis =====
    /** 1번 axis: HIRE_DATE / FISCAL_YEAR [SYS036] */
    private String axis1GrantBase;

    /** 2번 axis: 회계연도 시작월 (01~12, AXIS1=FISCAL_YEAR 시 필수) */
    private String axis2FiscalStartMm;

    /** 2번 axis: 회계연도 시작일 (01~31, AXIS1=FISCAL_YEAR 시 필수) */
    private String axis2FiscalStartDd;

    /** 3번 axis: MONTHLY_ONLY / PRORATE / NEXT_YEAR_BULK [SYS037] */
    private String axis3FirstYearMethod;

    /** 3번 보조: 입사일 일괄선부여 여부 (Y/N, 프리셋 4번 표현) */
    private String axis3PregrantYn;

    /** 4번 axis: CEIL / ROUND / FLOOR / HALF_DAY [SYS038] (AXIS3=PRORATE일 때만 유효, 그 외는 CEIL 강제) */
    private String axis4ProrateRounding;

    /** 5번 axis: LEGAL / CUSTOM */
    private String axis5TenureMode;

    /** 5번 axis: 가산 시작 연차 (1~3, LEGAL 시 3 강제) */
    private Integer axis5StartYear;

    /** 5번 axis: 가산 주기 (1~2, LEGAL 시 2 강제) */
    private Integer axis5Interval;

    /** 5번 axis: 최대 연차일수 (25 이상, 법정) */
    private Integer axis5MaxDays;

    /** 6번 axis: 유효기간(개월) 12 또는 24 */
    private Integer axis6ValidityMonths;

    /** 7번 axis: 사용촉진 Y/N */
    private String axis7UsePromotion;

    /** 법정연차 신청 결재 여부 (Y/N) — prafta-019-E 결정 #2 */
    private String aprvUseYn;

    // ===== 활성 관리 =====
    /** 활성 여부 (Y/N, 회사당 Y는 1건) */
    private String useYn;

    /** 정책 적용 시작일 (YYYYMMDD) */
    private String applyFromDate;

    /** 입력자 */
    private String insertNo;

    /** 입력일시 (ISO 8601 문자열 형태로 직렬화) */
    private String insertDate;

    /** 수정자 */
    private String updateNo;

    /** 수정일시 */
    private String updateDate;

    // ===== TB_LEAVE_USAGE_POLICY (1:1) =====
    /**
     * 회사 허용 사용 단위 (단일, prafta-024).
     * FULL_DAY / HALF_DAY / QUARTER_DAY / HOUR_2 / HOUR_1 / MIN_30 중 1개.
     * AXIS4=HALF_DAY(0.5일 단위 절사) 시 HALF_DAY 강제.
     * QUARTER_DAY(반반차, LC-10) 선택 시 허용집합 = 종일/반차/반반차 — 시간차는 허용되지 않는다.
     */
    private String usageUnit;

    /**
     * 반반차 허용 Y/N — <b>더 이상 입력값이 아니다</b>(LC-10).
     * USAGE_UNIT='QUARTER_DAY' 에서 파생되어 기록만 되며, 신청 게이팅은 USAGE_UNIT 을 직접 본다.
     * 구 이력 스냅샷과의 비교 연속성을 위해 컬럼/필드만 유지한다.
     */
    private String allowQuarter;

    /**
     * 짜투리 잔여 보전 옵션 Y/N (PC-05, D3).
     * 'Y' = 잔여 &lt; 최소 사용단위 요금일 때 최소단위 1건 사용 허용 + 부족분 회사 부담(TB_LEAVE_REMNANT_COVER 기록).
     * 'N'(기본) = 시스템 미개입 — 소멸 임박 짜투리 리포트(D9-③)로 지원.
     */
    private String allowRemnantRoundUp;
}
