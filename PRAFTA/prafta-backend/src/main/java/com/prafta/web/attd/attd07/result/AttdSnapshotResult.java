package com.prafta.web.attd.attd07.result;

/**
 * 승인 처리 직전 근태(TB_USER_ATTD_MGMT) 현재 출퇴근 스냅샷.
 *
 * <p>처리 이력(TB_USER_ATTD_HIST)의 "변경 전(BEF_*)" 값을 클라이언트 입력이 아니라
 * 서버 권위 데이터로 채우기 위해 사용한다(감사 무결성). 수정요청(REQ_TYPE='02')은
 * targetId 가 가리키는 기존 행의 현재값, 생성요청(REQ_TYPE='01')은 행이 없어 null 이다.
 */
public record AttdSnapshotResult(
      String checkInDate
    , String checkInTime
    , String checkOutDate
    , String checkOutTime
) {
}
