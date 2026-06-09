package com.prafta.app.tbm.admin.result;

/**
 * prafta-051 R-C(E13) 입실취소 물리삭제 직전 감사 스냅샷.
 *
 * <p>물리 DELETE 후에는 행이 사라져 추적 흔적이 app 로그뿐이므로, 삭제 직전 동일 트랜잭션 내에서
 * 대상 출결의 식별 코드값만 조회해 성공 로그에 남긴다(누가 누구를 취소했는지 사후 추적).
 * 평문 이름/좌표 등 PII 는 포함하지 않고 USER_CD/USER_TYPE_CD 코드값만 담는다.
 */
public record AdminCancelEntrySnapshotResult(
    String userCd
    , String userTypeCd
){
}
