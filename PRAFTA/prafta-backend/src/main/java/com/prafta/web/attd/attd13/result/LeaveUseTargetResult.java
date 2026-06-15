package com.prafta.web.attd.attd13.result;

/**
 * 변경/삭제 대상 연차 사용행 검증용 최소 결과 (PRAFTA-COM-008-C).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 */
public record LeaveUseTargetResult(
      String leaveId
    , String cmpnyCd
    , String siteCd
    , String userCd
    , String leaveCd
    , String grantId
    , String startDate
    , String useUnitType
    , String leaveStatus
) {
}
