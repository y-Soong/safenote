package com.prafta.app.leave.leaveflow.result;

/**
 * prafta-app-018-B: 연차 종류 메타(결재여부·허용단위 산출 입력).
 *
 * <p>웹 {@code com.prafta.web.attd.leaveflow.vo.LeaveTypeInfoVO} 미러.
 * ⚠️ MyBatis record 위치매핑 — SELECT 컬럼 순서(systemYn, aprvUseYn, useUnitType, leaveType, maxAplyDays, availTermType)와 일치해야 한다.
 *
 * <p>연차개편: 사용자 신청('01') 한도검증을 위해 {@code leaveType}/{@code maxAplyDays} 를 추가한다.
 *   maxAplyDays 는 tinyint unsigned NULL 허용 → Integer. '01'인데 NULL 이면 한도 0 = 신청불가(fail-closed).
 *
 * <p>prafta-com-016-B(3-1): 사용자 신청('01') 한도 윈도우 분기를 위해 {@code availTermType} 추가
 *   ('01':설정안함=전체누적 / '02':해당연도내=회계연도).
 *
 * <p>연차 신청 증빙 필수화(2026-08-29): {@code evidenceYn} 을 맨 끝에 추가(위치매핑 규약, 기존 6개 필드 순서 불변).
 */
public record LeaveTypeInfoRow(
      String systemYn
    , String aprvUseYn
    , String useUnitType
    , String leaveType
    , Integer maxAplyDays
    , String availTermType
    , String evidenceYn
) {
}
