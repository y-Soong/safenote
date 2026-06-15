package com.prafta.common.schedule.attdreminder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 출근/퇴근 5분 전 리마인더(W4/W5) 스케줄러 (PRAFTA-APP-021-4). <b>기본 비활성(게이트)</b>.
 *
 * <p>{@code prafta.attd-reminder.worker.enabled=true} 일 때만 동작한다(미설정/false면 매 실행 즉시 건너뜀).
 * 매분 0초(cron "0 * * * * *")에 실행해 (현재+5분)에 시작/종료하는 스케줄 보유자에게 리마인더를 적재한다.
 * 멱등 dedupKey 로 같은 분 재실행돼도 1건만 적재된다. com-002 FCM 워커({@code PushSendScheduler})의
 * 게이트/예외 처리 관례를 미러한다(cron 단위). {@code @EnableScheduling} 은 {@code MainApplication} 에 존재.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttdReminderScheduler {

    private final AttdReminderService attdReminderService;

    /** 게이트: 기본 false(비활성). 운영 검증 후 true 로 켠다(마이그/SYS045 시드 선적용 필수). */
    @Value("${prafta.attd-reminder.worker.enabled:false}")
    private boolean workerEnabled;

    /** 매분 0초 실행. 분 경계에서 (현재+5분) 시작/종료 스케줄을 매칭한다. */
    @Scheduled(cron = "${prafta.attd-reminder.worker.cron:0 * * * * *}")
    public void runReminderWorker() {
        if (!workerEnabled) {
            log.debug("출근/퇴근 리마인더 워커 비활성(prafta.attd-reminder.worker.enabled=false) — 건너뜀");
            return;
        }
        try {
            attdReminderService.dispatchReminders();
        } catch (Exception e) {
            // 워커 루프가 죽지 않도록 예외를 삼키고 다음 분 재시도.
            log.error("출근/퇴근 리마인더 워커 실행 중 예외 발생", e);
        }
    }
}
