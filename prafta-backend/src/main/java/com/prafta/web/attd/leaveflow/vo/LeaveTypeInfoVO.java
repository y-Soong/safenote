package com.prafta.web.attd.leaveflow.vo;

/**
 * 연차 타입 메타 (prafta-019-E 결재 여부/사용단위 판단용).
 *
 * @param systemYn   Y면 PRAFTA-018 법정 시드 타입 → 결재 여부는 tb_leave_policy.APRV_USE_YN 사용
 * @param aprvUseYn  회사정의 타입의 결재 여부 (tb_leave_type_mgmt.APRV_USE_YN)
 * @param useUnitType 타입에 설정된 사용 단위 [SYS025]
 */
public record LeaveTypeInfoVO(
      String systemYn
    , String aprvUseYn
    , String useUnitType
) {
}
