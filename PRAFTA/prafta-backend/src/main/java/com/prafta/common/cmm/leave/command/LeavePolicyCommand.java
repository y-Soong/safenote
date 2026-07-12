package com.prafta.common.cmm.leave.command;

/**
 * 정책 생성/변경 입력 객체.
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5.2 (7개 axis)
 *
 * <p>본 record는 baim07 모듈의 Param에서 변환되어 {@code LeavePolicyService}로 전달된다.
 * 회사 코드 및 사용자 코드는 별도 인자로 전달되므로 본 record에는 포함하지 않는다.
 *
 * <p>Cross-axis 검증은 {@code LeavePolicyServiceImpl}에서 수행한다. 본 record는
 * 단순 값 운반체로, NULL 허용 여부는 axis 조합에 따라 다르다(§8.5.3).
 *
 * @param policyPreset           프리셋 분류 (HIRE_DATE/FISCAL_PRORATE/FISCAL_MONTHLY/HIRE_DATE_PREGRANT/CUSTOM)
 * @param axis1GrantBase         1번 axis: HIRE_DATE/FISCAL_YEAR
 * @param axis2FiscalStartMm     2번 axis: 회계연도 시작월 (AXIS1=FISCAL_YEAR 시 필수)
 * @param axis2FiscalStartDd     2번 axis: 회계연도 시작일 (AXIS1=FISCAL_YEAR 시 필수)
 * @param axis3FirstYearMethod   3번 axis: MONTHLY_ONLY/PRORATE/NEXT_YEAR_BULK
 * @param axis3PregrantYn        3번 보조: Y/N (입사일 일괄선부여)
 * @param axis4ProrateRounding   4번 axis: CEIL/ROUND/FLOOR/HALF_DAY (AXIS3=PRORATE일 때만 의미)
 * @param axis5TenureMode        5번 axis: LEGAL/CUSTOM
 * @param axis5StartYear         5번 axis: 가산 시작 연차
 * @param axis5Interval          5번 axis: 가산 주기
 * @param axis5MaxDays           5번 axis: 최대 연차일수
 * @param axis6ValidityMonths    6번 axis: 12 또는 24
 * @param axis7UsePromotion      7번 axis: Y/N
 * @param aprvUseYn              법정연차 신청 결재 여부 (Y/N)
 * @param applyFromDate          정책 적용 시작일 (YYYYMMDD, 오늘 이상)
 * @param usageUnit              회사 허용 사용 단위 (단일): FULL_DAY/HALF_DAY/HOUR_2/HOUR_1/MIN_30 (prafta-024)
 * @param allowQuarter           반반차(0.25일, SYS025 '05') 허용 토글 Y/N — USAGE_UNIT 계층과 독립 (LC-06)
 * @param changeReason           변경 사유 (HISTORY 기록용, NULL 허용)
 */
public record LeavePolicyCommand(
      String policyPreset
    , String axis1GrantBase
    , String axis2FiscalStartMm
    , String axis2FiscalStartDd
    , String axis3FirstYearMethod
    , String axis3PregrantYn
    , String axis4ProrateRounding
    , String axis5TenureMode
    , Integer axis5StartYear
    , Integer axis5Interval
    , Integer axis5MaxDays
    , Integer axis6ValidityMonths
    , String axis7UsePromotion
    , String aprvUseYn
    , String applyFromDate
    , String usageUnit
    , String allowQuarter
    , String changeReason
) {
}
