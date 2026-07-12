package com.prafta.common.cmm.leave.vo;

/**
 * 연차 시간차 1일 환산시간 정책 행 (tb_leave_conversion_policy — LC-02, F4 effective-dating).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 *
 * @param applyFromDate    적용 시작일(YYYYMMDD) — 이 날짜 이후 신청 대상일(WORK_YMD)분부터 적용
 * @param dailyConvMinutes 1일 환산시간(분) — 시간차 차감 분모
 * @param insertNo         등록자(USER_CD)
 * @param insertDate       등록일시(ISO 문자열)
 * @param updateNo         수정자(USER_CD)
 * @param updateDate       수정일시(ISO 문자열)
 */
public record LeaveConversionPolicyVO(
      String applyFromDate
    , Integer dailyConvMinutes
    , String insertNo
    , String insertDate
    , String updateNo
    , String updateDate
) {
}
