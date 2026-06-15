package com.prafta.common.schedule.attdreminder;

/**
 * 출근/퇴근 5분 전 리마인더(W4/W5) outbox 적재 서비스 (PRAFTA-APP-021-4).
 *
 * <p>스케줄러가 매분 1회 호출한다. (현재시각+5분)에 시작/종료하는 스케줄 보유자를 산출해 멱등
 * dedupKey 로 outbox(PENDING) 1건씩 적재한다. 발송은 공용 FCM 워커 소관.
 */
public interface AttdReminderService {

    /**
     * 1주기 실행: 출근/퇴근 리마인더 대상을 각각 적재한다.
     *
     * @return 이번 주기에 적재한 outbox 건수(중복 흡수분 제외)
     */
    int dispatchReminders();
}
