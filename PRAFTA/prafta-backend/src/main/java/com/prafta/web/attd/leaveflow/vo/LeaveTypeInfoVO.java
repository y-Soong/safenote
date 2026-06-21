package com.prafta.web.attd.leaveflow.vo;

/**
 * 연차 타입 메타 (prafta-019-E 결재 여부/사용단위 판단용).
 *
 * <p>⚠️ MyBatis record 위치매핑 — SELECT 컬럼 순서(systemYn, aprvUseYn, useUnitType, leaveType, maxAplyDays, availTermType)와 일치.
 *
 * @param systemYn   Y면 PRAFTA-018 법정 시드 타입 → 결재 여부는 tb_leave_policy.APRV_USE_YN 사용
 * @param aprvUseYn  회사정의 타입의 결재 여부 (tb_leave_type_mgmt.APRV_USE_YN)
 * @param useUnitType 타입에 설정된 사용 단위 [SYS025]
 * @param leaveType  연차 타입 [SYS021] ('01':사용자 신청 / '02':관리자 부여·시스템 연차 등)
 * @param maxAplyDays 사용자 신청('01') 최대 신청일수(tinyint unsigned NULL → Integer). '01'인데 NULL이면 한도 0(신청불가)
 * @param availTermType 사용자 신청('01') 사용가능기간 [SYS026] ('01':설정안함=전체누적 / '02':해당연도내=회계연도). prafta-com-016-B(3-1)
 */
public record LeaveTypeInfoVO(
      String systemYn
    , String aprvUseYn
    , String useUnitType
    , String leaveType
    , Integer maxAplyDays
    , String availTermType
) {
}
