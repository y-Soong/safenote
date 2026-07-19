package com.prafta.web.user.user01.result;

/**
 * 사용자 대기요청 일괄 종결(F1/QT-11-7) 처리 건수 요약.
 *
 * <p>소속이동 발효({@code User01TransferExecutionService}) 및 사용자 비활성/탈퇴
 * ({@code User01Service})가 공유하는 {@code UserPendingRequestTerminationService}의 반환값이다.
 * 호출자가 로그(감사 추적)에 종결 건수를 남길 때 사용한다.
 *
 * @param applicantReqCancelled 신청자(대상자) 근태/연차/스케줄수정 요청 취소 건수(REQ_STATUS '01' → '04')
 * @param approverReqRejected   결재자(대상자) 배정 요청 본문 반려 건수('01' → '03')
 * @param approverStepRejected  결재자(대상자) 결재단계 반려 건수('00'/'01' → '03')
 * @param otCancelled           신청자(대상자) 진행중 초과근무 취소 건수('IN_PROGRESS' → 'CANCELLED')
 * @param leaveChangeRejected   연차변경요청 반려 건수(TARGET/INITIATOR 양방향, → 'REJECTED')
 * @param leaveLedgerRestored   종결된 연차('05') 요청 원장 원복 건수(use 취소 + GRANT 재집계)
 */
public record PendingRequestTerminationResult(
        int applicantReqCancelled,
        int approverReqRejected,
        int approverStepRejected,
        int otCancelled,
        int leaveChangeRejected,
        int leaveLedgerRestored
) {
}
