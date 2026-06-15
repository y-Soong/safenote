package com.prafta.app.leave.leave01.result;

import java.math.BigDecimal;

/**
 * 연차 개편(표시): 신청형 휴가('01') 타입별 1행 (연차 현황 화면 "신청형 휴가" 섹션 본체).
 *
 * <p>대상 = TB_LEAVE_TYPE_MGMT WHERE CMPNY_CD AND LEAVE_TYPE='01' AND USE_YN='Y'.
 *   법정연차/관리자부여(GRANT 그룹)와 절대 합산하지 않는 별도 항목이다.
 *
 * <p>⚠️ MyBatis 위치매핑: 생성자 인자 순서 = SELECT 컬럼 순서.
 *   {@code AppLeave01Mapper.selectAppliedLeaveTypes} 의 SELECT 절과 1:1 로 맞춘다.
 * <ul>
 *   <li>{@code leaveCd}     : 연차코드(TB_LEAVE_TYPE_MGMT.LEAVE_CD).</li>
 *   <li>{@code leaveNm}     : 연차명(TB_LEAVE_TYPE_MGMT.LEAVE_NM).</li>
 *   <li>{@code maxAplyDays} : 한도(MAX_APLY_DAYS, tinyint unsigned, NULL 가능 → Integer.
 *       NULL 이면 서비스에서 한도 0 = 잔여 0 으로 fail-closed 처리).</li>
 *   <li>{@code usedDays}    : 당해 회계연도 CONFIRMED 사용 합계(Σ LEAVE_DAYS). 없으면 0
 *       (IFNULL 로 0 보정). 잔여(remainDays)는 서비스에서 한도-사용으로 파생.</li>
 * </ul>
 */
public record AppliedLeaveTypeRow(
      String leaveCd
    , String leaveNm
    , Integer maxAplyDays
    , BigDecimal usedDays
) {
}
