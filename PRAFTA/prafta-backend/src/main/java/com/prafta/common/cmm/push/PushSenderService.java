package com.prafta.common.cmm.push;

/**
 * FCM 공용 PUSH 전송 서비스 (PRAFTA-COM-002, consumer).
 *
 * <p>{@code tb_noti_outbox} PENDING 행을 claim → 단말 토큰 조회 → FCM 전송 →
 * SENT/FAILED/PENDING(재시도) 상태전이한다. 스케줄러({@code PushSendScheduler})가 호출한다.
 */
public interface PushSenderService {

    /**
     * 1주기 발송 처리. 게이트/FCM 가용성 검사 후 배치 claim·전송·상태전이.
     *
     * @return 이번 주기에 SENT 로 전이된 행 수(관측/로그용).
     */
    int dispatchPending();
}
